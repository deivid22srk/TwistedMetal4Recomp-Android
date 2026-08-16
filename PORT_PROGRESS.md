
## Tentativa de obter a imagem

O link do Internet Archive iniciou um download parcial, mas a transferência estava incompleta no momento da troca de fonte. O novo link do CoolROM foi aberto pelo navegador e respondeu **Download link not valid**, informando que o link não é válido para esta conexão ou foi aberto a partir de outro site. Portanto, esse URL não pode ser tratado como uma fonte verificável sem gerar um novo link dentro da sessão do site.

A imagem esperada continua sendo a revisão USA Rev. 1 com CUE/BIN e 23 tracks, conforme `game.toml` e `disc_probe.json`.


## Estado do download

A página de downloads do navegador confirmou que `Twisted Metal 4.7z` está ativo, com aproximadamente **248 MB de 591 MB** recebidos, a cerca de **464 KB/s**, sem erro. O arquivo ainda não deve ser extraído nem validado até atingir 100%.


## Imagem validada

O download `/home/ubuntu/Downloads/Twisted Metal 4.7z` foi concluído com **619.547.404 bytes** e MD5 `af888b93f6df040f16abfeb23f262d5f`, coincidente com o metadado público do arquivo. A extração contém um CUE e as 23 tracks BIN.

A Track 01 extraída tem **384.615.504 bytes**, MD5 `0e5d822f108fcef31057645721d3d710` e SHA-1 `64e226413b27ea12639589b3ca2806976c795c8b`, coincidindo com `game.toml`/`disc_probe.json`. A imagem é, portanto, compatível com a revisão USA Rev. 1 exigida pelo projeto.

A imagem permanece fora do repositório e não será incluída no APK publicado.

## Porte Android e CI

O porte Android foi preparado com Gradle/NDK, SDL3 `SDLActivity`, seleção SAF da pasta CUE/BIN, cópia para o armazenamento privado do app, OpenBIOS redistribuível empacotado, gamepad Bluetooth/USB via SDL e fallback touch virtual. O runtime usa OpenGL ES e ABI arm64-v8a no primeiro APK.

As alterações do runtime psxrecomp foram vendorizadas no repositório do port para que o GitHub Actions compile sem depender de um gitlink modificado. Os fontes gerados do jogo e do OpenBIOS são versionados para o CI; a imagem original, CUE/BIN da cópia pessoal, caches e builds locais permanecem fora do staging. O workflow `.github/workflows/build.yml` instala SDK/NDK/CMake, pré-baixa SDL3 com SHA-256, compila `assembleRelease` e publica o APK como artifact.

A verificação real em dispositivo Android ainda depende de um aparelho/emulador externo disponível; o sandbox não possui um dispositivo Android conectado.


## Crash após seleção da pasta no Android

Log fornecido do Moto G34 5G / Android SDK 35: `libmain.so` e `libSDL3.so` carregavam corretamente, mas o thread SDL recebeu SIGSEGV dentro de `std::__ndk1::basic_filebuf::~basic_filebuf`, chamado por `PSXRecompV4::parse_cue_sheet` → `resolve_disc_path` → `sha256_file` → `mod_runtime_commit`, antes de `cdrom_init`. Os avisos sobre `libdolphin.so` e propriedades de display não eram a causa do crash.

A correção Android-only desativa o commit/fingerprint do gerenciador de mods no Android, que não é usado pelo launcher vanilla do APK, e deixa o caminho normal `cdrom_init` consumir a cópia CUE/BIN no armazenamento privado. O comportamento desktop permanece inalterado. O novo CI precisa confirmar esse caminho sem acessar o parser de CUE durante o commit de mods.

## Segundo log do dispositivo: encerramento normal antes da janela

O log `hhhhh15_08-20-01-21_862.log` não contém sinal fatal nem stack trace. Em duas tentativas, SDL carregou `libSDL3.so` e `libmain.so`, criou a superfície 1600x720 e iniciou `SDL_main`, mas `Finished main function` ocorreu aproximadamente 16 ms depois; em seguida a `GameActivity` foi destruída. Isso indica retorno antecipado do runtime, não crash de memória.

A causa foi identificada no caminho de BIOS. `GameActivity` passava `--game` e `--disc`, mas não passava `--bios`; o fallback `bios/openbios.bin` era resolvido relativo a `argv[0]`, que no SDL Android é o caminho da biblioteca em `/data/app/.../lib/arm64`, enquanto o OpenBIOS está em `Context.getFilesDir()/bios/openbios.bin`. O diretório padrão de memory cards também seguia `argv[0]`, potencialmente apontando para uma área somente leitura.

Correção aplicada em `GameActivity.getArguments()`: passar `--bios <filesDir>/bios/openbios.bin` e `--memcard-dir <filesDir>/saves` explicitamente, além dos argumentos já existentes. Os avisos sobre `libdolphin.so`, `vendor.display.enable_optimal_refresh_rate`, `Unknown dataspace 0` e driver Adreno são secundários e não explicam o encerramento.

## Terceiro log: retorno antecipado persiste

O log `psps15_08-20-12-35_863.log` ainda não contém SIGSEGV ou outro sinal fatal. Em duas execuções, `libSDL3.so`/`libmain.so` carregam, a superfície 1600x720 é criada e `SDL_main` termina normalmente cerca de 50 ms após iniciar. A correção Java dos argumentos foi compilada, mas o runtime ainda podia descartar o `--bios` explícito: `resolve_bios_for_runtime()` tratava builds com apenas OpenBIOS bundled como `bundled_only` e ignorava qualquer BIOS explicitamente solicitada antes de validar o arquivo.

O patch seguinte aceita no Android o caminho absoluto do OpenBIOS fornecido pelo `GameActivity` quando o arquivo existe e seu CRC/tamanho correspondem ao backend bundled. Também encaminha ao Logcat os argumentos efetivos, a resolução/identidade do BIOS e o caminho final do disco, além de vincular a biblioteca NDK `log`. Isso tornará visível a próxima condição de retorno caso o boot ainda não avance.

## Interface e input após o primeiro boot estável

O log `rodando15_08-20-39-39_428.log` confirma boot estável: BIOS e CUE são resolvidos, áudio AAudio abre corretamente e o jogo permanece ativo por mais de um minuto. A linha SDL `setOrientation() requestedOrientation=13 width=960 height=720 resizable=true` confirma que a janela estava em modo redimensionável, sem fullscreen. O código de touch existente apenas convertia dedos em botões; não havia nenhuma rotina de desenho do virtual gamepad. O default de release Android também deixava P1 como `keyboard`, enquanto somente builds de debug usavam `auto`.

Correções implementadas: `GameActivity` agora reaplica fullscreen imersivo e oculta barras de status/navegação; o runtime força `SDL_WINDOW_FULLSCREEN_DESKTOP` somente no Android; foi criada `TouchOverlayView`, uma camada visual com D-pad, face buttons, L1/R1, Select e Start que não intercepta os eventos SDL; P1 Android passa a usar `auto` para abrir o primeiro SDL GameController; e o Manifest/GameActivity solicitam Bluetooth Connect/Scan em Android 12+ para gamepads pareados. O runtime também registra no Logcat a contagem de joysticks, se cada dispositivo é GameController e qual controle foi aberto no P1.

## Layout touch revisado e visibilidade por gamepad

A captura fornecida mostrou que os botões grandes sobrepunham o HUD central e que L2/R2 não estavam disponíveis. O novo layout usa as barras pretas laterais do formato 20:9: D-pad no lado esquerdo, face buttons no lado direito, L2/L1 empilhados no topo esquerdo, R2/R1 no topo direito e Select/Start no topo central. A função nativa `android_touch_region()` foi alinhada às mesmas regiões normalizadas da `TouchOverlayView`, incluindo L2/R2.

A `GameActivity` agora consulta os dispositivos Android a cada 500 ms. Se existir uma fonte `SOURCE_GAMEPAD` ou `SOURCE_JOYSTICK`, o overlay fica `GONE`; quando todos os gamepads são desconectados, ele volta a `VISIBLE`. O SDL continua recebendo os eventos touch quando o overlay está visível, e o overlay não intercepta eventos.

## Branch de UI avançada: HUD, drawer e analógico virtual

A nova implementação está isolada na branch `android-ui-settings`, derivada do `master` estável. O `TouchOverlayView` foi redesenhado com cores discretas por botão PlayStation, contorno/glow, botão de abertura do drawer e modo visual de analógico. O `SettingsDrawerView` é um painel Android nativo sobre a Activity SDL e oferece fullscreen, visibilidade do HUD, D-pad/analógico, ocultação automática com gamepad e opacidade do HUD. As opções são salvas em `SharedPreferences` e aplicadas novamente na próxima execução.

O modo analógico não é apenas visual: o runtime recebe `--android-touch-mode`, mantém a posição do dedo na zona esquerda e converte o deslocamento em eixos DualShock com deadzone radial. A troca durante a execução usa JNI; fullscreen usa uma fila atômica processada na thread SDL para chamar `SDL_SetWindowFullscreen` com segurança. A branch será publicada e validada pelo `build.yml` antes de qualquer merge no `master`.

