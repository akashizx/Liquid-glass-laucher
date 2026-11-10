# Liquid Glass Launcher — Android TV

Primeiro MVP do launcher Liquid Glass para Android TV 11 (API 30), com tela inicial em Compose, navegação por foco/D-pad, descoberta de aplicativos instalados, abertura de apps, reconhecimento de voz Android e síntese de voz (TTS). Inclui backend Node.js para integração segura com a OpenAI Responses API.

## Estado desta entrega

- Implementado: projeto Android Studio, activity declarada como HOME/DEFAULT/LEANBACK_LAUNCHER, layout inicial Liquid Glass, descoberta e abertura de apps, reconhecimento de fala pt-BR, TTS pt-BR, backend HTTP para IA.
- Ainda necessário antes de uso real: substituir `ASSISTANT_BACKEND_URL` em `MainActivity.kt` pela URL pública HTTPS do backend, testar foco/compatibilidade no modelo exato da TV e validar compilação em Android Studio.
- Não é possível gerar um APK verificado neste ambiente porque não há Android SDK/Gradle wrapper configurado aqui.

## Requisitos

- Android Studio recente
- JDK 17
- Android SDK Platform 35 e Build Tools correspondentes
- TV ou emulador Android TV com API 30+

## Abrir o projeto

1. Extraia o ZIP.
2. Abra a pasta `liquidglass-launcher` no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Compile em `Build > Make Project`.
5. Instale em uma TV/emulador Android TV.

O app declara as categorias `HOME`, `DEFAULT` e `LEANBACK_LAUNCHER`. Após instalar, selecione **Liquid Glass Launcher** como aplicativo inicial padrão nas configurações de apps da TV, se o fabricante permitir a troca.

## Backend de IA

1. Entre em `backend/`.
2. Instale Node.js 20 ou superior.
3. Execute `npm install`.
4. Copie `.env.example` para `.env` e configure `OPENAI_API_KEY`.
5. Execute `npm start`.
6. Publique o backend atrás de HTTPS e configure rate limiting/autenticação antes de expor na internet.

Endpoint: `POST /api/assistant` com JSON `{ "message": "..." }`. O app Android envia a transcrição de voz para esse endpoint e lê a resposta usando TextToSpeech. Comandos simples iniciados por “abrir” tentam abrir localmente o app correspondente.

**Segurança:** nunca coloque a chave OpenAI no APK, no código Kotlin, em recursos Android ou no repositório. A chave deve permanecer exclusivamente no servidor. Consulte a documentação oficial: https://developers.openai.com/api/docs/guides/api-keys

## Observações de compatibilidade Android 11

- A listagem de apps usa `QUERY_ALL_PACKAGES`, sujeita às políticas de distribuição da Google Play. Para distribuição pública, prefira declarar consultas de pacotes específicas quando possível e revisar as políticas aplicáveis.
- Alguns fabricantes restringem a substituição do launcher padrão. O intent HOME permite que o Android ofereça o app como opção, mas não garante que a TV permita selecioná-lo.
- Reconhecimento de voz depende de serviço de reconhecimento instalado e permissão de microfone. O TTS depende dos mecanismos de voz disponíveis na TV.
- O wallpaper, metadados de recomendações e widgets de clima são etapas posteriores; esta versão usa uma composição visual original sem depender de serviços externos.


## Compilar pelo iPhone usando GitHub Actions

Este projeto inclui `.github/workflows/build-android.yml`. Não é necessário instalar Android Studio no iPhone.

1. Crie um repositório vazio no GitHub com o nome `liquidglass-launcher`.
2. Abra GitHub Codespaces pelo Safari: https://github.com/codespaces
3. Crie um Codespace para esse repositório.
4. No terminal do Codespaces, envie o ZIP do projeto para o ambiente (use o botão de upload do explorador de arquivos) e execute:
   `unzip liquidglass-launcher-iphone-ready.zip`
5. Copie o conteúdo da pasta extraída para a raiz do repositório, preservando a pasta `.github` e a pasta `liquidglass-launcher`.
6. Execute `git add . && git commit -m "Add Liquid Glass launcher" && git push`.
7. No GitHub, abra Actions > Build Android TV APK. Ao terminar, abra a execução e baixe o artefato `liquidglass-launcher-debug-apk`.

O artefato é um APK de depuração para testes, não uma versão assinada para publicação. A compilação em nuvem ainda precisa ser executada para confirmar se o código compila.
