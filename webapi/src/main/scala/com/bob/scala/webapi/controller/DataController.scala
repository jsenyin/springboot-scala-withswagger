package com.bob.scala.webapi.controller

import com.bob.java.webapi.service.DataService
import com.bob.scala.webapi.data.DataOptionService
import io.swagger.annotations.{Api, ApiOperation, ApiResponse, ApiResponses}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.VndErrors
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, ResponseStatus, RestController}

import scala.collection.JavaConverters._

/**
  * @Description:
  *
  * <p></p>
  * @author jsen.yin [jsen.yin@gmail.com]
  *         2019-03-01
  */
@RestController
@RequestMapping(value = Array("data"))
@Api(value = "data server", description = "data server")
class DataController {

  private val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  @Autowired
  private val dataOptionService: DataOptionService = null

  @RequestMapping(value = Array("join"), method = Array(RequestMethod.GET))
  @ApiOperation(value = "join List")
  @ResponseStatus(HttpStatus.OK)
  @ApiResponses(Array(
    new ApiResponse(code = 401, message = "无权限操作", response = classOf[VndErrors]),
    new ApiResponse(code = 404, message = "没有处理器", response = classOf[VndErrors]),
    new ApiResponse(code = 204, message = "记录不存在", response = classOf[VndErrors])))
  def join(): java.util.List[User] = {

    dataOptionService.computeDataFrame()



    val aUser = new User("c", 4, "a44", 4)
    val aList = List(new User("a", 1, "a11", 1), new User("b", 2, "b22", 2), new User("c", 3, "c33", 3))
    aList.+:(aUser).asJava
  }


}
