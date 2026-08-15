#include "psxrecomp_codegen_host.h"

void psxrecomp_codegen_host_apply(
    RecompLauncherCGameInfo* game_info,
    const PsxrecompCodegenHostConfig* config) {
    (void)game_info;
    (void)config;
}

int psxrecomp_codegen_host_sources_missing(
    const PsxrecompCodegenHostConfig* config) {
    (void)config;
    return 0;
}

void psxrecomp_codegen_host_relaunch_or_exit(const char* disc_path) {
    (void)disc_path;
}

void psxrecomp_codegen_host_forward_if_built(
    const PsxrecompCodegenHostConfig* config, int argc, char** argv) {
    (void)config;
    (void)argc;
    (void)argv;
}
