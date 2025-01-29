import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types._


object CoPurchaseAnalysis {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("CoPurchaseAnalysis")
      .config("spark.sql.shuffle.partitions", "200")
      .config("spark.default.parallelism", "100")
      .getOrCreate()

    val sc = spark.sparkContext

    // Parsing degli argomenti
    val inputPath = args.find(_.startsWith("input_path="))
      .getOrElse(throw new IllegalArgumentException("input_path non specificato"))
      .split("=")(1)

    val outputPath = args.find(_.startsWith("output_path="))
      .getOrElse(throw new IllegalArgumentException("output_path non specificato"))
      .split("=")(1)

    // Lettura del file CSV
    val rawData: RDD[String] = sc.textFile(inputPath)

    // Parsing delle righe in coppie (orderId, productId)
    val ordersAndProducts = rawData.map(line => {
      val Array(orderId, productId) = line.split(",").map(_.trim.toInt)
      (orderId, productId)
    })

    // Raggruppamento dei prodotti per ordine usando combinatori locali per ridurre il traffico di rete
    val groupedByOrder = ordersAndProducts.groupByKey()

    // Calcolo delle coppie di prodotti co-acquistati efficientemente
    val productPairs = groupedByOrder.flatMap { case (_, products) =>
      val productList = products.toSet.toList.sorted
      for {
        i <- productList.indices
        j <- i + 1 until productList.length
      } yield ((productList(i), productList(j)), 1)
    }

    // Riduzione con combinatori locali
    val coPurchaseCounts = productPairs
      .reduceByKey(_ + _)
      .map { case ((product1, product2), count) =>
        Row(product1, product2, count)
      }

    // Definizione dello schema per il DataFrame
    val schema = StructType(Seq(
      StructField("product1", IntegerType, nullable = false),
      StructField("product2", IntegerType, nullable = false),
      StructField("count", IntegerType, nullable = false)
    ))

    // Creazione del DataFrame
    val finalDF = spark.createDataFrame(coPurchaseCounts, schema)

    // Scrittura su un unico file
    finalDF.coalesce(1)
      .write
      .option("header", "false")
      .mode("overwrite")
      .csv(outputPath)
    spark.stop()
  }
}