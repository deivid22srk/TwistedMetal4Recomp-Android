
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

