import { test } from '@playwright/test';
import { makeCustomerPayload, registerCustomer, expectLoggedIn, login } from '../helpers/auth';
import { addAddressViaProfile, addCardViaProfile, addFirstBookToCartViaUI, checkoutAndFinalize } from '../helpers/checkout';

test('e2e: checkout creates order (RF0021–RF0037 smoke)', async ({ page }) => {
  const customer = makeCustomerPayload({ password: 'StrongPass!1' });

  await registerCustomer(page, customer);

  

  await expectLoggedIn(page);

  await addAddressViaProfile(page, {
    nickname: 'Casa E2E',
    street: 'Rua A',
    number: '100',
    neighborhood: 'Centro',
    city: 'Belo Horizonte',
    state: 'MG',
    zipCode: '30130000',
  });

  await addCardViaProfile(page, {
    cardholderName: customer.fullName,
    cardNumber: '4111111111111111',
    brand: 'VISA',
    expirationMonth: 12,
    expirationYear: 2030,
    preferred: true,
  });

  await addFirstBookToCartViaUI(page);
  await checkoutAndFinalize(page);
});