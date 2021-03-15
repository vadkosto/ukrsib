package com.dnsabr.vad.ukrsib.utils;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * Стандартный класс и методы для работы Streaming API for XML (StAX)
 * используется сервисом FileService для парсинга входящего XML-файла
 */
public class StaxStreamProcessor implements AutoCloseable {
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private final XMLStreamReader reader;

    public StaxStreamProcessor(InputStream is) throws XMLStreamException {
       reader = FACTORY.createXMLStreamReader(is,"UTF-8");
    }

    public XMLStreamReader getReader() {
       return reader;
    }

    @Override
    public void close() {
       if (reader != null) {
          try {
             reader.close();
          } catch (XMLStreamException e) {/*пустое*/}
       }
    }
}