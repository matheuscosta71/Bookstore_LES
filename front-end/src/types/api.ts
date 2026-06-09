export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type Book = {
  id: string;
  code?: string | null;
  title: string;
  author?: string | null;
  category?: string | null;
  categoryIds?: string[];
  categoryNames?: string[];
  price: number;
  costPrice?: number | null;
  pricingGroupId?: string | null;
  isbn: string;
  maxSaleValue?: number | null;
  stockQuantity: number;
  active: boolean;
  publicationYear?: number | null;
  edition?: string | null;
  pageCount?: number | null;
  synopsis?: string | null;
  heightCm?: number | null;
  widthCm?: number | null;
  depthCm?: number | null;
  weightKg?: number | null;
  barcode?: string | null;
  authorId?: string | null;
  publisherId?: string | null;
  supplierId?: string | null;
  lastLifecycleReason?: string | null;
  lastLifecycleJustification?: string | null;
};

export type CartItem = {
  id: string;
  bookId: string;
  title: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  expired: boolean;
  purchaseDisabled: boolean;
  /** RNF0045: próximo do fim do prazo do item */
  expiringSoon?: boolean;
  /** ISO-8601: fim da reserva (última atividade + minutos do sistema). RNF0042 — timer no front. */
  expiresAt?: string | null;
};

export type Cart = {
  id: string;
  status: string;
  totalAmount: number;
  freightAmount?: number | null;
  deliveryAddressId?: string | null;
  items: CartItem[];
  itemExpirationMinutes: number;
  /** Janela de aviso antes da expiração (minutos), alinhada ao back-end */
  expirationWarningMinutes?: number;
  hasExpiredItems: boolean;
  checkoutAllowed: boolean;
  /** RN0032: avisos após reconciliação com estoque */
  stockAdjustmentMessages?: string[];
  /** RNF0042: livros removidos do carrinho por expirar o prazo de reserva (itens já retirados na API). */
  reservationExpiredMessages?: string[];
};

export type Customer = {
  id: string;
  code?: string | null;
  fullName: string;
  email: string;
  cpf: string;
  phone: string;
  birthDate: string;
  active: boolean;
  /** RN0027 */
  rankingScore?: number;
};

export type Address = {
  id: string;
  nickname: string;
  street: string;
  number: string;
  complement?: string | null;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  type?: string;
  active: boolean;
};

export type CreditCard = {
  id: string;
  cardholderName: string;
  cardNumberMasked: string;
  brand: string;
  expirationMonth: number;
  expirationYear: number;
  preferred: boolean;
  active: boolean;
};

export type OrderItem = {
  id: string;
  bookId: string;
  title: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  /** Presente na API ao detalhar/listar pedidos; indica se já houve solicitação de troca no item */
  exchangeRequested?: boolean;
};

export type PaymentLine = {
  paymentType: string;
  amount: number;
};

export type Order = {
  id: string;
  /** Ex.: #A1B2C — derivado do id; não substitui o UUID interno */
  orderNumber?: string;
  customerName?: string;
  /** Em listagens admin */
  customerId?: string;
  status: string;
  freightAmount: number;
  itemsSubtotal: number;
  totalAmount: number;
  deliveryAddressId: string;
  createdAt: string;
  items: OrderItem[];
  payments: PaymentLine[];
  /** Cupom de troca após troca concluída (RF0044), se houver */
  exchangeCouponCode?: string | null;
};

export type FreightResponse = {
  freightAmount: number;
  itemsSubtotal: number;
  grandTotal: number;
};

export type SalesLineChart = {
  labels: string[];
  values: number[];
};

export type CategoryVolumeSeries = {
  category: string;
  volumes: number[];
};

export type SalesCategoryVolumeChart = {
  labels: string[];
  series: CategoryVolumeSeries[];
};

export type SalesSummary = {
  totalRevenue?: number;
  totalOrders?: number;
  averageTicket?: number;
  exchangeCount?: number;
};

/** Resposta de solicitação de troca (admin e cliente) */
export type ExchangeRequestResponse = {
  id: string;
  orderId: string;
  orderItemId: string;
  customerId: string;
  bookId: string;
  bookTitle?: string;
  orderStatus?: string;
  exchangeStatus?: string;
  returnToStock?: boolean | null;
  generatedCouponCode?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type AuditLogRow = {
  id: string;
  entityName: string;
  entityId: string;
  actionType: string;
  changedBy?: string | null;
  changedAt: string;
  changedData?: string | null;
};

export type InventoryBookRow = {
  bookId: string;
  title: string;
  isbn: string;
  category?: string | null;
  quantityAvailable: number;
  lastUpdatedAt?: string;
};

export type InventoryMovementRow = {
  id: string;
  bookId: string;
  bookTitle?: string | null;
  movementType: string;
  referenceType?: string | null;
  referenceId?: string | null;
  quantity: number;
  notes?: string | null;
  createdAt: string;
};
