import type { ManufacturingBomItemVO } from './process'

type Expect<T extends true> = T
type HasKey<T, K extends PropertyKey> = K extends keyof T ? true : false

type ProcessContractAssertions = [
  Expect<HasKey<ManufacturingBomItemVO, 'routeStepId'>>,
  Expect<HasKey<ManufacturingBomItemVO, 'processId'>>,
]

void (0 as unknown as ProcessContractAssertions)
