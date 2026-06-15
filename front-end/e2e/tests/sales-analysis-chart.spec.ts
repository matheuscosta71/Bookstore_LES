import { test, expect } from '@playwright/test';
import { adminLogin } from '../helpers/admin';

test.describe('RF0055 / Análise de Vendas com Gráfico de Linhas por Categoria', () => {
  test('deve renderizar o gráfico de linhas por categoria e período agrupado por mês', async ({ page }) => {
    // 1. Fazer login do administrador
    await adminLogin(page);

    // 2. Navegar para a página de analytics admin
    await page.goto('/admin/analytics');

    // Verificar se o painel carregou
    await expect(page.getByRole('heading', { name: /Dashboard de vendas/i })).toBeVisible();
    await expect(page.getByText(/Receita total/i)).toBeVisible({ timeout: 25_000 });

    // 2. Configurar o período de análise de pelo menos 13 meses
    const today = new Date();
    const start = new Date();
    start.setMonth(today.getMonth() - 14); // 14 meses atrás

    const startStr = start.toISOString().slice(0, 10);
    const endStr = today.toISOString().slice(0, 10);

    const startDateInput = page.locator('#analytics-start-date');
    const endDateInput = page.locator('#analytics-end-date');

    await startDateInput.fill(startStr);
    await endDateInput.fill(endStr);
    await page.locator('#analytics-apply-btn').click();

    // Aguardar que a requisição de atualização termine e os componentes re-renderizem
    await page.waitForTimeout(100);

    // 3. Validar se o gráfico de volume por categoria está visível
    const chartSection = page.locator('#sales-category-volume-section');
    await expect(chartSection).toBeVisible();
    await expect(chartSection.getByText('Volume de Vendas por Categoria')).toBeVisible();

    // 4. Validar se o gráfico de linhas foi renderizado e tem as linhas correspondentes
    const chartContainer = page.locator('#sales-category-volume-chart-container');
    await expect(chartContainer).toBeVisible();

    // Contar as linhas renderizadas no gráfico (devem coincidir com as categorias selecionadas por padrão)
    // Existem 5 categorias por padrão no seed do sistema: Ficção, Software, Negócios, Infantil, Literatura.
    const chartLines = chartContainer.locator('.recharts-line');
    await expect(chartLines).toHaveCount(5);

    // 5. Validar que o usuário consegue selecionar/deselecionar categorias
    const ficcaoCheckbox = page.locator('data-testid=category-checkbox-Ficção');
    const literaturaCheckbox = page.locator('data-testid=category-checkbox-Literatura');

    await expect(ficcaoCheckbox).toBeChecked();
    await expect(literaturaCheckbox).toBeChecked();

    // Deselecionar categoria "Ficção"
    await ficcaoCheckbox.uncheck();
    await expect(ficcaoCheckbox).not.toBeChecked();

    // Contagem de linhas no gráfico deve diminuir para 4
    await expect(chartLines).toHaveCount(4);

    // Deselecionar categoria "Literatura"
    await literaturaCheckbox.uncheck();
    await expect(literaturaCheckbox).not.toBeChecked();

    // Contagem de linhas no gráfico deve diminuir para 3
    await expect(chartLines).toHaveCount(3);

    // Re-selecionar categoria "Ficção"
    await ficcaoCheckbox.check();
    await expect(ficcaoCheckbox).toBeChecked();

    // Contagem de linhas no gráfico deve subir para 4
    await expect(chartLines).toHaveCount(4);

    // 6. Validar o agrupamento mensal
    // O eixo X deve apresentar os rótulos de meses (formato YYYY-MM) no período selecionado
    const xAxisTicks = chartContainer.locator('.recharts-cartesian-axis-tick-value');
    await expect(xAxisTicks.first()).toBeVisible({ timeout: 10_000 });
    const tickTexts = await xAxisTicks.allTextContents();
    
    // Verificar que há pelo menos 13 rótulos de meses
    expect(tickTexts.length).toBeGreaterThanOrEqual(13);
    
    // Validar se o formato dos rótulos corresponde ao padrão de meses (ex: "2025-05")
    const monthRegex = /^\d{4}-\d{2}$/;
    expect(tickTexts[0]).toMatch(monthRegex);
  });
});
