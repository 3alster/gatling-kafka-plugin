package ru.tinkoff.gatling.kafka.protocol

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import ru.tinkoff.gatling.kafka.protocol.KafkaProtocol._
import ru.tinkoff.gatling.kafka.request.KafkaProtocolMessage

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object KafkaProtocolBuilderNew {
  def producerSettings(ps: Map[String, AnyRef]): KPProducerSettingsStep = KPProducerSettingsStep(ps)

  case class KPProducerSettingsStep(producerSettings: Map[String, AnyRef]) {
    def consumeSettings(cs: Map[String, AnyRef]): KPConsumeSettingsStep = KPConsumeSettingsStep(producerSettings, cs)
  }

  case class KPConsumeSettingsStep(producerSettings: Map[String, AnyRef], consumeSettings: Map[String, AnyRef]) {

    def timeout(t: FiniteDuration): KafkaProtocolBuilderNew =
      KafkaProtocolBuilderNew(producerSettings, consumeSettings, t, skipMessages = 0)
    def withDefaultTimeout: KafkaProtocolBuilderNew         =
      KafkaProtocolBuilderNew(producerSettings, consumeSettings, 60.seconds, skipMessages = 0)
  }
}

case class KafkaProtocolBuilderNew(
    producerSettings: Map[String, AnyRef],
    consumeSettings: Map[String, AnyRef],
    timeout: FiniteDuration,
    messageMatcher: KafkaMatcher = KafkaKeyMatcher,
    skipMessages: Int,
) extends {

  def matchByValue: KafkaProtocolBuilderNew =
    messageMatcher(KafkaValueMatcher)

  def matchByMessage(keyExtractor: KafkaProtocolMessage => Array[Byte]): KafkaProtocolBuilderNew =
    messageMatcher(KafkaMessageMatcher(keyExtractor))

  private def messageMatcher(matcher: KafkaMatcher): KafkaProtocolBuilderNew =
    copy(messageMatcher = matcher)
  def skipMatches(n: Int): KafkaProtocolBuilderNew                           =
    copy(skipMessages = n)

  def build: KafkaProtocol = {

    val serializers = Map(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG   -> "org.apache.kafka.common.serialization.ByteArraySerializer",
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG -> "org.apache.kafka.common.serialization.ByteArraySerializer",
    )

    val consumeDefaults = Map(
      StreamsConfig.APPLICATION_ID_CONFIG            -> s"gatling-test-${java.util.UUID.randomUUID()}",
      StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG   -> Serdes.ByteArray().getClass.getName,
      StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG -> Serdes.ByteArray().getClass.getName,
    )

    KafkaProtocol(
      "test",
      producerSettings ++ serializers,
      consumeDefaults ++ consumeSettings,
      timeout,
      messageMatcher,
      skipMessages,
    )
  }
}
