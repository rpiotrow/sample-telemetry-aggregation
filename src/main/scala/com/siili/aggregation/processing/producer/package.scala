package com.siili.aggregation.processing

import zio.Has

package object producer {

  type KafkaSender = Has[KafkaSender.Service]

}
