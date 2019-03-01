package com.bob.scala.webapi.config

import org.springframework.context.annotation.{Configuration, ImportResource}

/**
  * @Description:
  *
  * <p></p>
  * @author jsen.yin [jsen.yin@gmail.com]
  *         2019-02-28
  */
@Configuration
@ImportResource(locations = Array("classpath*:spring/dubbo/*.xml"))
class DubboConfig {

}
