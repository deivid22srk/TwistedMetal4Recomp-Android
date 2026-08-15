# Port de Twisted Metal 4 Recomp para Android

## Resultado

O port Android foi publicado no repositório privado [deivid22srk/TwistedMetal4Recomp-Android](https://github.com/deivid22srk/TwistedMetal4Recomp-Android). O build final foi executado pelo GitHub Actions na revisão `5f11325a9fb098e7e8edfd510a93ce8eb112126b` e terminou com **success** na execução [31912707408](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/actions/runs/31912707408).

O workflow contém uma etapa explícita **Build Native library (CMake/NDK)**, que compila `libmain.so` e `libSDL3.so` para `arm64-v8a`, uma etapa de verificação ELF, staging das bibliotecas em `app/src/main/jniLibs/arm64-v8a`, empacotamento do APK e verificação dos dois arquivos nativos dentro do APK.

## Download

O APK instalável está publicado como asset da release [android-build-2eec61ec8d63](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/tag/android-build-2eec61ec8d63). O link direto é:

[Baixar app-release.apk](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/download/android-build-2eec61ec8d63/app-release.apk)

A quota de armazenamento de artifacts do GitHub Actions estava esgotada. Por isso, o workflow final publica o APK como **GitHub Release asset**, que foi validado com sucesso; a etapa de artifact foi removida para que a execução final não contenha falhas anotadas por uma limitação de armazenamento externo ao código.

## Assets do jogo

A imagem utilizada durante a geração foi a cópia USA Rev. 1 fornecida pelo usuário. Ela foi validada localmente antes da geração: CUE/BIN com 23 tracks, Track 01 com 384.615.504 bytes, MD5 `0e5d822f108fcef31057645721d3d710` e SHA-1 `64e226413b27ea12639589b3ca2806976c795c8b`. A imagem original não foi incluída no repositório nem no APK.

O app usa o Storage Access Framework para o usuário selecionar a pasta que contém o CUE/BIN. Os arquivos são copiados para o armazenamento privado do app. O OpenBIOS redistribuível é incluído; um BIOS retail não é necessário para o caminho padrão.

## Verificações realizadas

| Verificação | Resultado |
|---|---|
| Fontes gerados do jogo | Aprovado; 18 unidades `SCUS_945.60_full_*.c`, dispatch e headers presentes. |
| Build nativo CMake/NDK | Aprovado no GitHub Actions. |
| Bibliotecas nativas | `libmain.so` e `libSDL3.so`, ELF AArch64, presentes e empacotadas em `lib/arm64-v8a/`. |
| Contexto gráfico Android | Link `GLESv3`/`EGL` e perfil SDL OpenGL ES 3.0 configurados. |
| APK | Empacotado com sucesso. |
| Assinatura | `apksigner` confirmou V1 e V2 válidos, com um signer. É uma assinatura debug de CI para instalação/teste. |
| Worktree/repositório | Worktree local limpo e branch `master` sincronizada com o GitHub. |
| Dispositivo Android real | Não verificado: não havia aparelho conectado por `adb` nem emulador configurado no sandbox. |

## Observação sobre assinatura de produção

O APK publicado é instalável para testes porque usa a chave debug padrão do Android. Para distribuição pública ou atualização posterior no mesmo canal, deve ser configurado um keystore de produção em secrets do GitHub e trocado o `signingConfig` no `android/app/build.gradle`.

## Arquivos principais

O workflow está em `.github/workflows/build.yml`. A arquitetura e os riscos estão documentados em `ANDROID_PORT_ARCHITECTURE.md`, e o estado recuperável do trabalho está em `PORT_PROGRESS.md`.

## Referências

[1]: https://github.com/deivid22srk/TwistedMetal4Recomp-Android "Repositório do port Android"
[2]: https://github.com/deivid22srk/TwistedMetal4Recomp-Android/actions/runs/31912707408 "Execução final do GitHub Actions"
[3]: https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/tag/android-build-2eec61ec8d63 "Release asset do APK"

## Correção do crash após selecionar a pasta

O log do Moto G34 5G mostrou que SDL carregava corretamente `libSDL3.so` e `libmain.so`, mas o thread nativo sofria SIGSEGV em `std::__ndk1::basic_filebuf::~basic_filebuf`, dentro de `PSXRecompV4::parse_cue_sheet` chamado por `resolve_disc_path`/`sha256_file` durante `mod_runtime_commit`, antes de `cdrom_init`. Os avisos de `libdolphin.so` e propriedades de display não eram a causa do encerramento.

A correção Android-only agora pula o fingerprint/parser do gerenciador de mods no boot vanilla Android e deixa `cdrom_init` consumir diretamente a cópia local CUE/BIN. O comportamento desktop permanece inalterado. A revisão `d4655a517402a6c853bfcec259af2f1a8f40d11a` foi compilada com sucesso na execução [31913392450](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/actions/runs/31913392450).

O APK corrigido está na release [android-build-d4655a517402](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/tag/android-build-d4655a517402), com download direto em [app-release.apk](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/download/android-build-d4655a517402/app-release.apk). A assinatura V1/V2 foi verificada com `apksigner`; SHA-256: `bd2321db55bdfe1d1349c76e1efda5c2c7d9fafc0970a81dc92aabdc2042ee7f`.

O APK precisa ser testado novamente no Moto G34. O sandbox não tem um dispositivo Android conectado para executar a instalação e confirmar o boot real.

## Correção do retorno antecipado do SDL_main

O log `hhhhh15_08-20-01-21_862.log` não continha SIGSEGV: SDL carregava as bibliotecas, criava a superfície 1600x720, iniciava `SDL_main` e recebia `Finished main function` cerca de 16 ms depois. A causa foi o caminho do OpenBIOS. `GameActivity` passava `--game` e `--disc`, mas não passava `--bios`; o fallback relativo `bios/openbios.bin` era procurado junto à biblioteca em `/data/app/.../lib/arm64`, não em `filesDir/bios/`.

A revisão `3a899caeee178f2b54e45ea09fe505faa0ba2c92` passa explicitamente `--bios <filesDir>/bios/openbios.bin` e `--memcard-dir <filesDir>/saves`. Ela foi compilada e publicada com sucesso na execução [31913835915](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/actions/runs/31913835915).

A nova release está em [android-build-3a899caeee17](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/tag/android-build-3a899caeee17), com download direto em [app-release.apk](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/download/android-build-3a899caeee17/app-release.apk). O APK contém `assets/bios/openbios.bin` de 524.288 bytes, `libmain.so` e `libSDL3.so` em `lib/arm64-v8a/`; `apksigner` confirmou V1 e V2 válidos. SHA-256: `dc0779e7e9e8a064293beca1a6160abb2b1b108dc09e6d69a7ef11ab962705b0`.

## Correção da política bundled_only e diagnóstico Logcat

O terceiro log `psps15_08-20-12-35_863.log` ainda mostrava `SDL_main` encerrando normalmente, sem SIGSEGV. A análise do código revelou que, mesmo com `GameActivity` passando `--bios`, `resolve_bios_for_runtime()` descartava qualquer seleção explícita em builds que vinculam apenas o OpenBIOS bundled (`bundled_only`), antes de validar o arquivo. No Android, isso era incorreto porque o caminho explícito aponta para a cópia legítima do OpenBIOS em `filesDir`.

A revisão `c30b6f95bb2695c57fd19610a11d01e26f8a86d0` aceita o BIOS Android explícito somente quando o arquivo existe e corresponde ao backend OpenBIOS bundled por identidade, e adiciona mensagens `TwistedMetal4` ao Logcat para argumentos, resolução do BIOS e resolução do disco. O target Android também passou a vincular a biblioteca NDK `log`. A execução [31914333628](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/actions/runs/31914333628) terminou com sucesso.

O APK está na release [android-build-c30b6f95bb26](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/tag/android-build-c30b6f95bb26), com download direto em [app-release.apk](https://github.com/deivid22srk/TwistedMetal4Recomp-Android/releases/download/android-build-c30b6f95bb26/app-release.apk). O pacote contém `assets/bios/openbios.bin` de 524.288 bytes e as bibliotecas AArch64; a assinatura V1/V2 foi verificada. SHA-256: `bb7eb10ca73acb8cba4176d0317cf0ff25e17408da2a6d12f28912f6c53acb96`.
