#include <stddef.h>

/* The Android bootstrapper selects a disc through SAF before launching the
 * native runtime, so the desktop codegen relaunch callback is unreachable.
 * Keep the codegen host linkable in no-launcher builds without pulling the
 * desktop recomp-ui ABI into the APK. */
int recomp_launcher_relaunch_exe(char* out, size_t out_cap) {
    if (out && out_cap) out[0] = '\0';
    return 0;
}
