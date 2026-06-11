import type {
  RequisitionDTO,
  RequisitionItemDTO,
  RequisitionItemVO,
  RequisitionQuery,
  RequisitionVO,
} from './material-mgmt'

type Expect<T extends true> = T
type HasKey<T, K extends PropertyKey> = K extends keyof T ? true : false

type RequisitionContractAssertions = [
  Expect<HasKey<RequisitionDTO, 'items'>>,
  Expect<HasKey<RequisitionDTO, 'actualStartTime'>>,
  Expect<HasKey<RequisitionDTO, 'actualEndTime'>>,
  Expect<HasKey<RequisitionDTO, 'salesOrderLine'>>,
  Expect<HasKey<RequisitionVO, 'items'>>,
  Expect<HasKey<RequisitionVO, 'qualifiedQty'>>,
  Expect<HasKey<RequisitionItemDTO, 'issueQty'>>,
  Expect<HasKey<RequisitionItemVO, 'pendingQty'>>,
  Expect<HasKey<RequisitionQuery, 'productCode'>>,
]

void (0 as unknown as RequisitionContractAssertions)
