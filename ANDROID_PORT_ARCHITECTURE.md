# Twisted Metal 4 Recomp — Arquitetura do porte Android

## Objetivo

O porte será um APK Android nativo baseado no **SDL3 3.4.10**, mantendo o runtime PSXRecomp e o código C/C++ recompilado do jogo. O target nativo Android será uma biblioteca compartilhada chamada `main`, carregada pela `SDLActivity` padrão do SDL3. Os builds desktop continuarão produzindo o executável existente.

## Decisões

| Área | Decisão | Motivo |
|---|---|---|
| Build | Gradle + Android Gradle Plugin + CMake + NDK r28c; `compileSdk/targetSdk 35`, `minSdk 21`, ABI inicial `arm64-v8a`. | É o fluxo oficial do SDL3 Android e reduz a superfície de toolchain para o primeiro APK. |
| SDL | SDL3 3.4.10, preferencialmente compilado estaticamente dentro do `libmain.so`. | O runtime já usa SDL3 por padrão e o CMake fixa a versão e o SHA-256. |
| Entry point | `psx-runtime` vira `add_library(main SHARED ...)` somente quando `ANDROID`; a Activity usa `SDL_main`. | SDLActivity carrega `libSDL3.so` e `libmain.so`, inicia o thread nativo e gerencia ciclo de vida. |
| Launcher | Primeiro APK usa `--no-launcher` e inicia diretamente o jogo. | `recomp-ui` usa diálogos de arquivo/assumptions desktop e aumentaria o risco de tela preta ou crash inicial; a seleção da imagem será nativa Android. |
| Assets | `game.toml`, OpenBIOS, licença, ícone e configs são copiados de `assets/` do APK para `Context.getFilesDir()` antes de chamar o nativo. | O runtime resolve paths relativos a partir de `argv[0]`; o armazenamento privado evita scoped-storage e permissões amplas. |
| Disco | O usuário escolhe uma pasta através de `ACTION_OPEN_DOCUMENT_TREE`; o app copia o CUE e todas as 23 BINs referenciadas para `filesDir/disc/`, preservando nomes. | A CUE multi-track precisa das BINs vizinhas; copiar o conjunto evita depender de paths externos, permissões persistentes ou arquivos comprimidos. |
| Runtime | O argumento nativo será `--game <filesDir>/game.toml --disc <filesDir>/disc/Twisted Metal 4 (USA) (Rev 1).cue --no-launcher`. | Evita cwd/paths case-sensitive e garante que o CD-ROM seja montado com a imagem selecionada. |
| Vídeo | OpenGL ES via SDL; fallback software mantido. Vulkan ficará desabilitado no primeiro build Android. | O `game.toml` usa OpenGL e o backend SDL/OpenGL é o caminho de menor risco em Android. |
| Áudio | Manter `SDL_AudioStream` do SDL3 com 44,1 kHz e aceitar a frequência real do dispositivo. | O runtime já abstrai o áudio e usa o modelo pull compatível com Android. |
| Input | Gamepad Bluetooth/USB via SDL como prioridade; teclado físico também funciona. Touch fallback será implementado em uma camada nativa mínima por regiões virtuais, sem substituir o mapeamento de gamepad. | O runtime atual cobre GameController, mas não possui eventos de toque para controles; o fallback é necessário para jogabilidade sem controle físico. |
| Orientação | Landscape, proporção 4:3 com letterboxing. | Compatível com o `game.toml` e com a geometria original do PS1. |

## Fluxo de assets

A imagem original validada não será versionada, incorporada ao APK nem enviada ao GitHub. Durante desenvolvimento local, ela será usada para gerar `generated/` a partir de `psxrecomp_cli.py`. Em runtime Android, o app aceitará a pasta escolhida pelo usuário e armazenará uma cópia privada do CUE/BIN. O APK conterá somente o OpenBIOS redistribuível já presente no submódulo e sua licença.

## Riscos principais

1. O repositório não contém `generated/`; sem executar a geração com a imagem validada o target Android não será jogável.
2. A função CMake atual cria um executável desktop; o porte precisa trocar a forma do target no Android e garantir que `SDL_main` seja exportado no nome esperado.
3. `std::filesystem`, arquivos graváveis, `settings.toml`, `saves/` e caches precisam apontar para `filesDir`, nunca para `assets://` ou para um path absoluto externo.
4. O launcher Dear ImGui e os diálogos de arquivo não são necessários no primeiro APK e podem introduzir dependências de desktop; serão desativados para reduzir o risco inicial.
5. O renderer OpenGL usa contexto criado no thread principal do runtime; perda/restauração de Activity deve ser exercitada com pausa/retomada. O SDL3 `SDLActivity` será mantido sem fork desnecessário.
6. O runtime não trata atualmente eventos SDL de toque como controles PSX; o fallback touch precisa ser isolado para não alterar o caminho de gamepad.
7. A validação real de instalação/jogabilidade depende de um dispositivo Android ou emulador com `adb`; se não houver um conectado, o CI poderá comprovar o APK e a inspeção estática, mas não uma abertura real.

## Critério de aceite

O primeiro marco é: `./gradlew assembleRelease` gera APK arm64, o APK instala sem erro, o logcat mostra `SDLActivity` carregando `libSDL3`/`libmain`, o runtime entra no jogo sem launcher, monta a CUE validada, apresenta vídeo e inicializa áudio. Depois serão avaliados gamepad, touch, pausa/retomada e saves.

## Referências externas consultadas

- SDL3 Android README: https://wiki.libsdl.org/SDL3/README-android — documenta Android SDK 35+, NDK r28c+, API mínima 21, `SDLActivity`, `libmain.so`, integração CMake/Gradle, assets e ciclo de vida.
- SDL3 Android walkthrough: https://wiki.libsdl.org/SDL3/Android — descreve a construção Android e o wrapper SDL.
- SDL3 CMake documentation: https://github.com/libsdl-org/SDL/blob/main/docs/README-cmake.md — confirma suporte CMake para Android e o target `SDL3::SDL3`.
- Template oficial SDL3 3.4.10: https://github.com/libsdl-org/SDL/tree/release-3.4.10/android-project — fonte da Activity/JNI copiada para `android/`.
- `SDLActivity.java` 3.4.10: https://raw.githubusercontent.com/libsdl-org/SDL/release-3.4.10/android-project/app/src/main/java/org/libsdl/app/SDLActivity.java — confirma carregamento padrão de `SDL3` e `main` e chamada de `SDL_main`.
- `app/build.gradle` 3.4.10: https://raw.githubusercontent.com/libsdl-org/SDL/release-3.4.10/android-project/app/build.gradle — confirma `compileSdkVersion 35`, `ndkVersion 28.2.13676358`, `minSdkVersion 21` e ABI `arm64-v8a` no template.
