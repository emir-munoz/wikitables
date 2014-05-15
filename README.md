# Extracting RDF Relations from Wikipedia’s Tables

This project aims to extract relationships as RDF triples from tables found in Wikipedia's articles [Link](http://emir-munoz.github.com/wikitables).

## Abstract

We propose that existing RDF knowledge-bases can be leveraged to extract facts (in the form of RDF triples) from relational HTML tables on the Web with high accuracy. 
In particular, we propose methods using the DBpedia knowledge-base to extract facts from tables embedded in Wikipedia articles (henceforth "Wikitables"), effectively
enriching DBpedia with additional triples. We first survey the Wikitables from a recent dump of Wikipedia to see how much raw data can potentially be exploited by our methods.
We then propose methods to extract RDF from these tables: we map table cells to DBpedia entities and, for cells in the same row, we isolate a set of candidate 
relationships based on existing information in DBpedia for other rows. To improve accuracy, we investigate various machine learning methods to classify extracted triples
as correct or incorrect. We ultimately extract 7.9 million unique novel triples from one million Wikitables at an estimated precision of 81.5%.

## Specifications

All the codes are written using Java 6.0 and eclipse framework. To compile, each package contains a `build.xml` file to be used by ant.
We use English-language data from [DBpedia v3.8](http://wiki.dbpedia.org/Downloads38), describing 9.4 million entities. For answering queries that looks for relation 
between a pair of resources, we use local indexes of this DBpedia knowledge-base, and for each pair, we perform two atomic on-disk lookups for relations in either direction.
Important: The used indexes are not included in the repository given their size (ca. 9G), but are available under request. See contact information.

## Packages

The system is modularized into the following packages: 

### wikitables-demo
The web application built using Spring MVC that integrates the extraction and classification of RDF triples.

### wikitables-engine
The core or engine that performs the extraction of the RDF triples.

### wikitables-ml
For a given set of extracted triples, this performs the prediction and returns correct or incorrect label for each triple.

### wikitables-dal
Contains the classes that represent the model of the entire application and the access to indexes.

### wikitables-evaluation
Extract statistics from DBpedia to help in the features definition. 

### wikititles-index
An index used to fill the autocomplete data in the web application. 

## Machine learning

We also make available in this repository the training set used to build our machine learning models comprising 503 examples in two formats.
These can be used to validate our results and try new machine learning schemas.
`wikitables-training-set` file shows the feature vectors extracted for each example which are formatted using SVMLight format as follows:

```bash
<line> .=. <target> <feature>:<value> <feature>:<value> ... <feature>:<value> # <info>
<target> .=. +1 | -1
<feature> .=. <integer>
<value> .=. <float>
<info> .=. <string> 
```
where the target value and each of the feature/value pairs are separated by a space character. The <code>&lt;info&gt;</code> field contains the URL from where the 
example cames from and the <code>&lt;s,p,o&gt;</code> RDF triple. We also publish an ARFF version `wikitables-training-set.arff` to be used in Weka.

## How to use it?

You can deploy the demo `wikitables-demo-release-1.0` in for example a Tomcat 6 server. Follow this checklist:

1. Update the paths in the file `build.properties` according your tomcat configuration.
2. Update the paths in the file `/WebContent/WEB-INF` with the paths to the root folder, models and indexes.
3. Build all the packages with the supplied script `makeWikitables.sh`.
4. Build and copy the web project into tomcat using `ant deploy`.
5. Restart your tomcat.
6. Go to http://localhost/wikitables-demo-1.0 in your browser.

### Demostration

We have developed an on-line [demo](http://deri-srvgal25.nuigalway.ie:8080/wikitables-demo) of our approach, where we extract RDF relations for a given Wikipedia article. 
Our system receives a Wikipedia article's title as parameter and uses a selected (or default) machine-learning model to filter the best candidate triples.  
Go to our [demo](http://deri-srvgal25.nuigalway.ie:8080/wikitables-demo/) page, search for some article already in Wikipedia, select a model see how it works.

### License

The program can be used under the terms of the [Apache License, 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

## Contact

Please do not hesitate to contact us if you have any further questions about this project:  
Emir Muñoz <emir.munoz@gmail.com> and Aidan Hogan <aidan.hogan@deri.org>  
[Digital Enterprise Research Institute](http://deri.ie/)  
[National University of Ireland](http://www.nuigalway.ie/)  
Galway, Ireland  
