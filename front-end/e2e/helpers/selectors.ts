export const Sel = {
  headings: {
    register: /Criar conta/i,
    login: /Entrar/i,
    cart: /Carrinho de compras/i,
    checkout: /Checkout/i,
    profileOrders: /Meus pedidos/i,
    profileTransactions: /Extrato/i,
    profileAddresses: /Endereços/i,
    profileCards: /Cartões/i,
  },
  buttons: {
    cadastrar: /Cadastrar/i,
    entrar: /Entrar/i,
    adicionarAoCarrinho: /Adicionar ao carrinho/i,
    calcularFrete: /Calcular frete/i,
    finalizarCompra: /Finalizar compra/i,
  },
  inputs: {
    fullName: 'input[name="fullName"]',
    email: 'input[name="email"]',
    cpf: 'input[name="cpf"]',
    phone: 'input[name="phone"]',
    birthDate: 'input[name="birthDate"]',
    password: 'input[name="password"]',
    confirmPassword: 'input[name="confirmPassword"]',

    nickname: 'input[name="nickname"]',
    street: 'input[name="street"]',
    number: 'input[name="number"]',
    neighborhood: 'input[name="neighborhood"]',
    city: 'input[name="city"]',
    state: 'input[name="state"]',
    zipCode: 'input[name="zipCode"]',

    cardholderName: 'input[name="cardholderName"]',
    cardNumber: 'input[name="cardNumber"]',
    brand: 'input[name="brand"]',
    expirationMonth: 'input[name="expirationMonth"]',
    expirationYear: 'input[name="expirationYear"]',
  },
} as const;

