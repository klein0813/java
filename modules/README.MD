# Modules List

## fileParser
> 介绍
  * 借助第三方可执行程序(soffice)实现文件类型转换
> 问题
  * 限制打开的第三方可执行程序个数
    * 方案
    ```
      设置 static Semaphore，已限制并发量
    ```

    [CallSofficeSemaphore.java](fileParser\async\CallSofficeSemaphore.java)

  * 设置第三方可执行程序的执行超时时间
    * 方案
    ```
      检查进程状态和进程已执行时间，若进程已结束或超出预设的超时时间，则 destroy 该当前进程，否则睡眠0.1秒后再次检查
    ```
    * 代码位置

    [ConvertUtil.java](fileParser\utils\ConvertUtil.java)
> 入口

  [CallSofficeSemaphore.java-toConvert](fileParser\async\CallSofficeSemaphore)

## wordParser
> 介绍
  * 实现从文档中提取关键信息，暂支持 pdf，doc，docx，html，mht，图片(.png|.jpg|.jpeg|.bmp)，提取姓名，电话号码和邮箱
> 方案
  * 如何从文档中提取文本，解析方式
    * doc ：HWPFDocument.getDocumentText
    * docx: 未找到 jar 能完美的提取文本，从 docx 格式结构出发提取文本
    * pdf : PDFParser, PDDocumentPDF, TextStripper, 
    * html: Jsoup.parse
    * mht : 先将 mht 转换为 html 后，再做解析
    * img : 百度 AI，https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token=ACCESS_TOKENT
  * 如何从文本中提取关键信息
    * 从文档名中提取
    * 手机号码和邮箱通过正则获取
    * 姓名通过文档文本格式正则获取(简历的格式绝大部分是在一定的格式范围内)
> 问题
  * doc 文档的实际格式可能为：doc、docx、html、mht，即文档实际格式与其后缀表示格式可能不一致
    * 方案
    ```
      1. 通过 HWPFDocument 获取 doc 格式文本数据，若抛出异常，则视为伪 doc 文档
      2. 捕获 IllegalArgumentException，通过 Jsoup 再次获取文本数据
      3. 通过数据中的关键信息以判别其确切格式，docx（含有“[Content_Types].xml”，“word/document.xml”等信息）
        mht（“/(?i)MIME-Version:/”），否则为 html
    ```
  * 康熙字符对姓名提取的影响，身份证号码和邮箱对手机号码的影响
  * 文档文本格式正则目前是手动添加，不能代码自动添加！
> 入口

  [NPEParserUtil.java-parser](wordParser\NPEParserUtil.java)

> 补充

  [WebPageDataCrawlingUtil](wordParser\WebPageDataCrawlingUtil.java): 借助 `htmlunit` 实现网页文本提取