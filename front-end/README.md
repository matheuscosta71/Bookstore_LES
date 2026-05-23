# Livraria Matheus GN — Frontend

React + Redux Toolkit + Vite + Tailwind CSS, consumindo o backend Spring Boot em `../back-end`.

## Requisitos

- Node 18+
- Backend em `http://localhost:8080` (ou ajuste o proxy)

## Configuração

```bash
cp .env.example .env
# VITE_API_URL=/api — o Vite faz proxy para localhost:8080 (veja vite.config.ts)
# VITE_ADMIN_KEY — mesma chave do admin no backend (analytics)
```

## Scripts

```bash
npm install
npm run dev      # http://localhost:5173
npm run build
npm run preview
```

## Arquitetura

- `src/app/` — store Redux e hooks tipados
- `src/features/` — slices por domínio (`auth`, `books`, `cart`, `checkout`, `customer`, `ai`, `analytics`)
- `src/services/` — cliente Axios e chamadas à API
- `src/pages/` — rotas/páginas
- `src/components/` — UI reutilizável
- `src/layouts/` — Main, Auth, Profile, Admin

## Autenticação

O backend não expõe `POST /login` com JWT. O fluxo implementado:

1. **Cadastro** — `POST /customers`
2. **Login** — `GET /customers?email=...` + `GET /customers/{id}` e persistência do `customerId` em `localStorage`

A senha digitada no login não é validada no servidor neste modo (limitação da API atual).

## Extrato do cliente (RF0025)

Após **`POST /customers/{id}/checkout/finalize`**, o backend grava uma linha **PURCHASE** (valor total com frete). O front consome **`GET /customers/{id}/transactions`** e exibe na área **Perfil → Extrato**. Após finalizar uma compra, o estado Redux de pedidos e transações é atualizado automaticamente.

## Proxy

Em desenvolvimento, `/api/*` é encaminhado para `http://localhost:8080/*` (sem o prefixo `/api`).
