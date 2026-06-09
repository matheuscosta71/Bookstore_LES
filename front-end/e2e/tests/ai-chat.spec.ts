import { test, expect } from '@playwright/test';

test.describe('Chatbot de Recomendação de Produtos com IA', () => {
  test('Cenário 1 — Recomendação de produto compatível', async ({ page }) => {
    const mockReply = "Para aprender programação, recomendo o livro **Clean Code** que está cadastrado na nossa base de dados.";
    
    // Intercepta a API de chat para retornar a sugestão de produto cadastrado
    await page.route('**/api/ai/chat', async (route) => {
      const request = route.request();
      const payload = JSON.parse(request.postData() || '{}');
      
      // Verifica se a pergunta do usuário está correta
      expect(payload.message).toContain('Quero aprender programação');
      
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ reply: mockReply }),
      });
    });

    // Acessa a página de chat da IA
    await page.goto('/ai/chat');

    // Preenche o campo de mensagem
    const input = page.getByPlaceholder('Digite sua mensagem...');
    await expect(input).toBeVisible();
    await input.fill('Quero aprender programação');

    // Clica no botão de enviar
    const sendButton = page.getByRole('button', { name: /Enviar/i });
    await expect(sendButton).toBeVisible();
    await sendButton.click();

    // Valida que a mensagem enviada pelo usuário e a resposta da IA aparecem na tela
    await expect(page.getByText('Quero aprender programação')).toBeVisible();
    await expect(page.getByText('Para aprender programação, recomendo o livro Clean Code')).toBeVisible();
  });

  test('Cenário 2 — Ausência de produto compatível', async ({ page }) => {
    const mockReply = "Não encontrei recomendações compatíveis com a sua solicitação. Tente pesquisar por outros temas, como tecnologia, negócios ou ficção.";
    
    // Intercepta a API de chat para retornar a mensagem de ausência de produto compatível
    await page.route('**/api/ai/chat', async (route) => {
      const request = route.request();
      const payload = JSON.parse(request.postData() || '{}');
      
      // Verifica se a pergunta do usuário está correta
      expect(payload.message).toContain('Quero comprar uma bicicleta');
      
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ reply: mockReply }),
      });
    });

    // Acessa a página de chat da IA
    await page.goto('/ai/chat');

    // Preenche o campo de mensagem
    const input = page.getByPlaceholder('Digite sua mensagem...');
    await expect(input).toBeVisible();
    await input.fill('Quero comprar uma bicicleta');

    // Clica no botão de enviar
    const sendButton = page.getByRole('button', { name: /Enviar/i });
    await expect(sendButton).toBeVisible();
    await sendButton.click();

    // Valida que a mensagem do usuário e a resposta de fallback aparecem na tela
    await expect(page.getByText('Quero comprar uma bicicleta')).toBeVisible();
    await expect(page.getByText('Não encontrei recomendações compatíveis')).toBeVisible();
  });
});
