cmake_minimum_required(VERSION 3.20)
project(TwistedMetal4Recomp LANGUAGES C CXX)

set(PSXRECOMP_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/psxrecomp" CACHE PATH
    "Path to psxrecomp")

# --- Opt-in product surfaces (OFF by default in the framework) -------------
# Scaffold prompts / flags fill these. Without recomp-ui, PSX_RECOMP_UI is OFF.
# Setup-host CI requires WIZARD_CMAKE_BLOCK / ENABLE_SETUP_WIZARD (FORCE_SETUP_HOST
# alone does not open first-run / Generate & rebuild).
# recomp-ui submodule present (PSX_RECOMP_UI defaults ON).
# Full netplay UI — enable after adding recomp-ui + testing:
# if(EXISTS "${PSXRECOMP_ROOT}/lib/recomp-net/CMakeLists.txt")
#     set(PSX_NETPLAY ON CACHE BOOL "…" FORCE)
# endif()
set(PSX_SETUP_WIZARD ON CACHE BOOL
    "Advertise first-run setup wizard + Generate & rebuild in recomp-ui" FORCE)

# Android is driven by the SDL3 Activity and the native document picker in
# android/app. The desktop Dear ImGui launcher and Vulkan backend are not part
# of the first mobile target; keeping them off avoids desktop file-dialog and
# window-system assumptions in the APK while leaving desktop builds unchanged.
if(ANDROID)
    set(PSX_RECOMP_UI OFF CACHE BOOL
        "Disable the desktop recomp-ui launcher on Android" FORCE)
    set(PSX_ENABLE_VULKAN OFF CACHE BOOL
        "Disable Vulkan for the first Android target" FORCE)
    set(PSX_DEBUG_TOOLS OFF CACHE BOOL
        "Disable TCP debug tools in Android release builds" FORCE)
    set(PSX_STATIC_RUNTIME OFF CACHE BOOL
        "Use the Android C++ runtime configuration" FORCE)
    set(PSXRECOMP_REQUIRE_GAME_C ON CACHE BOOL
        "Require generated game C for a playable Android APK" FORCE)
endif()
include("${PSXRECOMP_ROOT}/runtime/runtime.cmake")

psxrecomp_add_game_runtime(psx-runtime
    # ENABLE_NETPLAY_IF_PRESENT
    # NETPLAY_LOBBY_URL "ws://netplay.retcomm.net:8765"
    ENABLE_SETUP_WIZARD
    WINDOW_TITLE "TwistedMetal4 Recompiled"
    DEFAULT_GAME_CONFIG_PATH "game.toml"
    MAX_PLAYERS 4
    LAUNCHER_BOXART "${CMAKE_CURRENT_SOURCE_DIR}/launcher_assets/img/boxart.tga"
    APP_ICON "${CMAKE_CURRENT_SOURCE_DIR}/assets/psxrecomp.ico"
    GEN_MARKER "generated/SCUS_945.60_dispatch.c"
    GEN_FULL_GLOB "generated/SCUS_945.60_full_*.c"
    CODEGEN_SETUP_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/codegen_setup.c"
)

# psxrecomp_codegen_host.c is shared with the desktop setup-host and references
# the launcher relaunch ABI. Android intentionally has no desktop launcher; a
# local no-op keeps that optional host code linkable without importing ImGui.
if(NOT PSX_RECOMP_UI)
    target_sources(psx-runtime PRIVATE
        "${CMAKE_CURRENT_SOURCE_DIR}/android/recomp_launcher_stub.c")
endif()

# Stage runtime configs next to the exe for direct build/ launches. Only copy
# files that exist — never fail POST_BUILD when game_options.toml is absent.
foreach(_PSX_RUNTIME_CONFIG game.toml game_options.toml)
    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/${_PSX_RUNTIME_CONFIG}")
        add_custom_command(TARGET psx-runtime POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                "${CMAKE_CURRENT_SOURCE_DIR}/${_PSX_RUNTIME_CONFIG}"
                "$<TARGET_FILE_DIR:psx-runtime>/${_PSX_RUNTIME_CONFIG}"
            COMMENT "Staging ${_PSX_RUNTIME_CONFIG}"
            VERBATIM)
    endif()
endforeach()

