# Web Crawler
___
## _How does search engine work?_
Search Engine, which is written on Java using Spring Boot and MySQL.
ОIt independently crawls web pages, indexes and finds more relevant ones to the user’s request.

![Java](https://img.shields.io/badge/Java-23-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)

## _Possibilities_
- Indexing web pages and storing data in MySQL
- Support for multi-threaded indexing
- Search by indexed data taking into account relevance
- Generating snippets in search results
- REST API for interacting with the system

## _Requirements_
- Java 17+
- Maven 3+
- MySQL 8+

## _How to start the application?_
Firstly you need to create database search_engine
```
CREATE DATABASE search_engine
```
In __application.yml__ you need to write your username and password for your MySql
```
spring:
  datasource:
    username: root
    password: rootuser
```
In __pom.xml__ in blocks with jar files
```
   <dependency>
            <groupId>custom.libs</groupId>
            <artifactId>dictionary-reader-1.5</artifactId>
            <version>1.0</version>
            <scope>system</scope>
            <systemPath>/Users/iulialadaniuc/Desktop/webCrawler/libs/dictionary-reader-1.5.jar</systemPath>
        </dependency>
```
in <systemPath></systemPath> for these files you should write file's paths.Reload maven and start application.
 
When application is started successfully, you should send GET HTTP-request on path /startIndexing. Engine starts indexing sites and pages.
If you want to stop indexing you should send GET HTTP-request on path /stopIndexing without any params. If you want to index page
separately you should send POST HTTP-request on path /indexPage with argument:
```
{
    "url": "site's url"
}
``` 
For searching you should send GET HTTP-request
on path /search with arguments in url:
``` 
http://localhost:8087/api/search?query=your query&site=site's url
``` 
You'll get response with pages and page's content which may match your request

