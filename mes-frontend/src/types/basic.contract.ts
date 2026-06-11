import type { MaterialPriceDTO, MaterialPriceQuery, MaterialPriceVO } from './basic'

type Expect<T extends true> = T
type HasKey<T, K extends PropertyKey> = K extends keyof T ? true : false

type MaterialPriceContractAssertions = [
  Expect<HasKey<MaterialPriceDTO, 'unitPrice'>>,
  Expect<HasKey<MaterialPriceDTO, 'unit'>>,
  Expect<HasKey<MaterialPriceVO, 'unitPrice'>>,
  Expect<HasKey<MaterialPriceVO, 'unit'>>,
  Expect<HasKey<MaterialPriceQuery, 'materialCode'>>,
  Expect<HasKey<MaterialPriceQuery, 'materialName'>>,
]

void (0 as unknown as MaterialPriceContractAssertions)
