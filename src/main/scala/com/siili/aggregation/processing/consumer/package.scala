package com.siili.aggregation.processing

import zio.Has

package object consumer {
  type KafkaReceiver = Has[KafkaReceiver.Service]
}
