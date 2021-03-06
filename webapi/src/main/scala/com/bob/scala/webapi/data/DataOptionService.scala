package com.bob.scala.webapi.data

import com.bob.scala.webapi.config.SparkConfig
import com.nqtown.util.FinalResult
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


/**
  * @Description:
  *
  * <p></p>
  * @author jsen.yin [jsen.yin@gmail.com]
  *         2019-03-01
  */
@Service
class DataOptionService {

  private val logger = LoggerFactory.getLogger(getClass)


  def computeLogisticReg(): FinalResult[List] = {

//    val conf = new SparkConf().setMaster("local").setAppName("LogisticRegExample")
//    val sc = new SparkContext(conf)
//    val sqlContext = new SQLContext(sc)

    val sqlContext = SparkConfig.getContext("LogisticRegExample")

    val training = sqlContext.createDataFrame(Seq(
      (1.0, Vectors.dense(0.0, 1.1, 0.1)),
      (0.0, Vectors.dense(2.0, 1.0, -1.0)),
      (0.0, Vectors.dense(2.0, 1.3, 1.0)),
      (1.0, Vectors.dense(0.0, 1.2, -0.5))
    )).toDF("label", "features")

    // 构建一个 LogisticRegression instance. 这个 instance 是一个 Estimator（用于训练，生成 model，model 是一个 Transformer）
    val lr = new LogisticRegression()

    // 打印参数，文档和默认的 values 等信息
    println("LogisticRegression parameters:\n" + lr.explainParams() + "\n")

    // 使用 setter methods 设置 parameters，当然也可以通过 ParamMap 设置
    lr.setMaxIter(10).setRegParam(0.01)

    // Estimator 调用 fit method 来进行训练，这里是学习一个 LogisticRegression model
    val model1 = lr.fit(training)

    // 可以查看模型用到的一些参数，注意会打印出 ID，这是 LogisticRegression instance 的唯一 ID
    println("Model 1 was fit using parameters: " + model1.parent.extractParamMap)

    // 可以使用 ParamMap 来设置参数
    val paramMap = ParamMap(lr.maxIter -> 30) // 会覆盖上面的 maxIter 参数.
      .put(lr.regParam -> 0.1, lr.threshold -> 0.55) // 设置多个 Params.

    // 对参数进行 combine
    val paramMap2 = ParamMap(lr.probabilityCol -> "myProbability") // 改变输出字段的名称
    val paramMapCombined = paramMap ++ paramMap2

    // 使用 paramMapCombined 参数进行训练，paramMapCombined 会覆盖前面所有的参数
    val model2 = lr.fit(training, paramMapCombined)
    println("Model 2 was fit using parameters: " + model2.parent.extractParamMap)

    // 测试数据
    val test = sqlContext.createDataFrame(Seq(
      (1.0, Vectors.dense(-1.0, 1.5, 1.3)),
      (0.0, Vectors.dense(3.0, 2.0, -0.1)),
      (1.0, Vectors.dense(0.0, 2.2, -1.5))
    )).toDF("label", "features")

    // 模型的预测是使用 Transformer.transform()，LogisticRegression.transform 只会使用 'features' 字段，
    // 注意 model2.transform() 的 'myProbability' 字段，而没有使用 'probability' 字段，这是由于前面我们
    // 使用了 lr.probabilityCol 参数
    model2.transform(test)
      .select("features", "label", "myProbability", "prediction")
      .collect()
      .foreach { case Row(features: Vector, label: Double, prob: Vector, prediction: Double) =>
        println(s"($features, $label) -> prob=$prob, prediction=$prediction")
      }

    sc.stop()


  }


  def computeDataFrame(): FinalResult[List] = {

    val app = "computeDataFrame";

    var config = SparkConfig.getConfig(app)

    val finalResult: FinalResult[List] = FinalResult[List]

    return finalResult

  }


}
