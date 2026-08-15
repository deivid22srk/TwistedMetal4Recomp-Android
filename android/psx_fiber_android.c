/* Android fiber backend.
 *
 * Bionic does not ship getcontext/makecontext/swapcontext. The PSX scheduler
 * still needs cooperative stackful contexts, so each guest fiber gets one
 * pthread with an explicit stack size and a semaphore hand-off. At most one
 * fiber is runnable at a time; the semaphore pair is therefore equivalent to
 * a cooperative context switch while remaining supported by the NDK.
 */
#include "psx_fiber.h"

#include <errno.h>
#include <pthread.h>
#include <semaphore.h>
#include <stdint.h>
#include <stdlib.h>

#if defined(PTHREAD_STACK_MIN)
#define PSX_ANDROID_FIBER_MIN_STACK ((size_t)PTHREAD_STACK_MIN)
#else
#define PSX_ANDROID_FIBER_MIN_STACK ((size_t)(256u * 1024u))
#endif

typedef struct psx_android_fiber {
    sem_t resume;
    pthread_t thread;
    int worker;
    int stop;
    psx_fiber_entry entry;
    void* arg;
} psx_android_fiber;

static _Thread_local psx_android_fiber* s_current;

static int wait_uninterrupted(sem_t* semaphore) {
    int rc;
    do {
        rc = sem_wait(semaphore);
    } while (rc != 0 && errno == EINTR);
    return rc;
}

static void* psx_android_fiber_thread(void* opaque) {
    psx_android_fiber* fiber = (psx_android_fiber*)opaque;
    s_current = fiber;
    if (wait_uninterrupted(&fiber->resume) != 0 || fiber->stop) {
        s_current = NULL;
        return NULL;
    }
    fiber->entry(fiber->arg);
    /* A guest thread fiber must return by switching to its scheduler target. */
    abort();
}

psx_fiber_t psx_fiber_convert_thread(void) {
    if (!s_current) {
        psx_android_fiber* fiber =
            (psx_android_fiber*)calloc(1, sizeof(*fiber));
        if (!fiber || sem_init(&fiber->resume, 0, 0) != 0) {
            free(fiber);
            return NULL;
        }
        fiber->thread = pthread_self();
        fiber->worker = 0;
        s_current = fiber;
    }
    return (psx_fiber_t)s_current;
}

psx_fiber_t psx_fiber_current(void) {
    return (psx_fiber_t)s_current;
}

psx_fiber_t psx_fiber_create(size_t stack_size, psx_fiber_entry entry, void* arg) {
    if (!entry) return NULL;
    if (stack_size < PSX_ANDROID_FIBER_MIN_STACK)
        stack_size = PSX_ANDROID_FIBER_MIN_STACK;

    psx_android_fiber* fiber =
        (psx_android_fiber*)calloc(1, sizeof(*fiber));
    if (!fiber) return NULL;
    if (sem_init(&fiber->resume, 0, 0) != 0) {
        free(fiber);
        return NULL;
    }
    fiber->worker = 1;
    fiber->entry = entry;
    fiber->arg = arg;

    pthread_attr_t attr;
    if (pthread_attr_init(&attr) != 0) {
        sem_destroy(&fiber->resume);
        free(fiber);
        return NULL;
    }
    if (pthread_attr_setstacksize(&attr, stack_size) != 0 ||
        pthread_create(&fiber->thread, &attr,
                       psx_android_fiber_thread, fiber) != 0) {
        pthread_attr_destroy(&attr);
        sem_destroy(&fiber->resume);
        free(fiber);
        return NULL;
    }
    pthread_attr_destroy(&attr);
    return (psx_fiber_t)fiber;
}

void psx_fiber_switch(psx_fiber_t target) {
    psx_android_fiber* to = (psx_android_fiber*)target;
    psx_android_fiber* from = s_current;
    if (!to || !from || to == from || to->stop) return;
    if (sem_post(&to->resume) != 0) abort();
    if (wait_uninterrupted(&from->resume) != 0) abort();
}

void psx_fiber_destroy(psx_fiber_t fiber) {
    psx_android_fiber* target = (psx_android_fiber*)fiber;
    if (!target || target == s_current) return;
    target->stop = 1;
    (void)sem_post(&target->resume);
    if (target->worker) (void)pthread_join(target->thread, NULL);
    sem_destroy(&target->resume);
    free(target);
}
