
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

