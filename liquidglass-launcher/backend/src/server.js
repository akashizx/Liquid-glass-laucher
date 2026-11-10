import 'dotenv/config';
import express from 'express';
import OpenAI from 'openai';

const app = express();
app.use(express.json({ limit: '32kb' }));
app.get('/health', (_req, res) => res.json({ ok: true, service: 'liquidglass-assistant' }));

app.post('/api/assistant', async (req, res) => {
  try {
    const message = String(req.body?.message ?? '').trim();
    if (!message || message.length > 4000) return res.status(400).json({ error: 'Informe uma mensagem de até 4.000 caracteres.' });
    if (!process.env.OPENAI_API_KEY) return res.status(503).json({ error: 'OPENAI_API_KEY não configurada no servidor.' });

    const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
    const response = await client.responses.create({
      model: process.env.OPENAI_MODEL || 'gpt-6-astra',
      instructions: 'Você é o assistente de voz do Liquid Glass Launcher, um launcher para Android TV. Responda em português brasileiro, de forma clara, concisa e adequada para ser ouvida em voz alta. Não afirme que executou ações no dispositivo; o aplicativo Android é responsável por isso.',
      input: message,
      max_output_tokens: 350
    });
    res.json({ answer: response.output_text || 'Não consegui gerar uma resposta.' });
  } catch (error) {
    console.error('assistant_error', error?.status || error?.message || error);
    res.status(502).json({ error: 'Não foi possível consultar o serviço de IA.' });
  }
});

const port = Number(process.env.PORT || 3000);
app.listen(port, '0.0.0.0', () => console.log(`Liquid Glass Assistant API listening on ${port}`));
