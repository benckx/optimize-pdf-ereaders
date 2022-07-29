<a href="https://paypal.me/benckx/2">
<img src="https://img.shields.io/badge/Donate-PayPal-green.svg"/>
</a>

# About

Some PDFs books found online are usually poorly rendered on small e-readers (e.g. Kindle Oasis).

This lib uses OCR to correct the angle, crop around the text and re-paginate in order for the best reading experience on
small e-readers.

## Examples

### Example 1

#### Input

<p float="left">
    <img src="thumbs/baudrillard_input_page_1.jpg"/>
    <img src="thumbs/baudrillard_input_page_2.jpg"/>
    <img src="thumbs/baudrillard_input_page_3.jpg"/>
    <img src="thumbs/baudrillard_input_page_4.jpg"/>
</p>

#### Output

<p float="left">
    <img src="thumbs/baudrillard_output_page_1.jpg"/>
    <img src="thumbs/baudrillard_output_page_2.jpg"/>
    <img src="thumbs/baudrillard_output_page_3.jpg"/>
    <img src="thumbs/baudrillard_output_page_4.jpg"/>
    <img src="thumbs/baudrillard_output_page_5.jpg"/>
    <img src="thumbs/baudrillard_output_page_6.jpg"/>
</p>

### Example 2

#### Input

<p float="left">
    <img src="thumbs/edinburgh_input_page_1.jpg"/>
    <img src="thumbs/edinburgh_input_page_2.jpg"/>
    <img src="thumbs/edinburgh_input_page_3.jpg"/>
    <img src="thumbs/edinburgh_input_page_4.jpg"/>
</p>

#### Output

<p float="left">
    <img style="border-width: 1px;border-color: red" src="thumbs/edinburgh_output_page_1.jpg"/>
    <img src="thumbs/edinburgh_output_page_2.jpg"/>
    <img src="thumbs/edinburgh_output_page_3.jpg"/>
    <img src="thumbs/edinburgh_output_page_4.jpg"/>
    <img src="thumbs/edinburgh_output_page_5.jpg"/>
    <img src="thumbs/edinburgh_output_page_6.jpg"/>
    <img src="thumbs/edinburgh_output_page_7.jpg"/>
    <img src="thumbs/edinburgh_output_page_8.jpg"/>
    <img src="thumbs/edinburgh_output_page_9.jpg"/>
    <img src="thumbs/edinburgh_output_page_10.jpg"/>
    <img src="thumbs/edinburgh_output_page_11.jpg"/>
    <img src="thumbs/edinburgh_output_page_12.jpg"/>
    <img src="thumbs/edinburgh_output_page_13.jpg"/>
    <img src="thumbs/edinburgh_output_page_14.jpg"/>
    <img src="thumbs/edinburgh_output_page_15.jpg"/>
    <img src="thumbs/edinburgh_output_page_16.jpg"/>
    <img src="thumbs/edinburgh_output_page_17.jpg"/>
    <img src="thumbs/edinburgh_output_page_18.jpg"/>
    <img src="thumbs/edinburgh_output_page_19.jpg"/>
    <img src="thumbs/edinburgh_output_page_20.jpg"/>
    <img src="thumbs/edinburgh_output_page_21.jpg"/>
    <img src="thumbs/edinburgh_output_page_22.jpg"/>
    <img src="thumbs/edinburgh_output_page_23.jpg"/>
    <img src="thumbs/edinburgh_output_page_24.jpg"/>
    <img src="thumbs/edinburgh_output_page_25.jpg"/>
    <img src="thumbs/edinburgh_output_page_26.jpg"/>
    <img src="thumbs/edinburgh_output_page_27.jpg"/>
</p>

# Requirements

```shell
sudo apt-get install tesseract-ocr
```

# Usage

```java
        RequestConfig requestConfig = RequestConfig
            .builder()
            .pdfFile(file)
            .minPage(minPage)
            .maxPage(maxPage)
            .correctAngle(true)
            .build();

        Processor processor=new Processor(requestConfig);
        processor.process();
        processor.joinThread();
        File outputFile=processor.writeToPDFFile(fileName+"optimized.pdf");
```

# TODO

* Move to Gradle
* Move to Kotlin
* Finish picture detection
* Create a user-friendly runnable
* Re-add tests
