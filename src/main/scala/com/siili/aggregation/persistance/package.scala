package com.siili.aggregation

import zio.Has

package object persistance {

  type AggregationRepo = Has[AggregationRepo.Service]

}
