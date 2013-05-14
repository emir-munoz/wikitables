# Extracting RDF Relations from Wikipedia’s Tables

This project aims to extract relationships as RDF triples from tables found in Wikipedia's articles. [Link]

## Abstract

We propose that existing knowledge-bases can be leveraged to extract high-quality facts (in the form of RDF triples) from relational HTML tables on the Web. 
We focus on using the DBpedia knowledge-base to extract facts from tables embedded in Wikipedia articles (henceforth "Wikitables"), for which we can leverage 
some potential benefits over the general Web-table case. First, many Wikitables contain wikilinks that effectively disambiguate DBpedia entities referred to 
in cells. Second, the context of the table can be resolved per the article that contains it. Third, we can directly use a legacy knowledge-base (in our case DBpedia) 
to mine preexisting relations within individual rows of the tables, using them as candidates to identify new relationships and facts aligned with the reference 
entity-set and schema. We first survey the Wikitables available in a recent dump of Wikipedia to see how much raw data can potentially be exploited by our methods. 
We then propose methods to extract RDF from these tables: we map table cells to DBpedia resources and, for cells in the same row, we isolate a set of candidate 
relationships based on existing information in DBpedia for other rows. In order to boost precision, we propose a number of ranking functions for selecting candidate 
relations during the extraction of RDF. We apply our methods over a recent Wikipedia dump containing over a million tables, where we extract 24 million triples at 
an estimated accuracy of 50%, and subsequently boost that accuracy to 65% by using machine learning methods to classify 19 million triples that are deemed likely to be correct.

## Machine learning

We make available in this repository the training and testing sets used to build our models comprising 1,000 examples altogether.
These can be used to validate our results and try new machine learning schemas.
The files shows the feature vectors for each example which are formatted using SVMLight format.

```bash
<line> .=. <target> <feature>:<value> <feature>:<value> ... <feature>:<value> # <info>
<target> .=. +1 | -1
<feature> .=. <integer>
<value> .=. <float>
<info> .=. <string> 
```
where the target value and each of the feature/value pairs are separated by a space character. The <code>&lt;info&gt;</code> field contains the URL from where the 
example cames from and the <code>&lt;s,p,o&gt;</code> RDF triple.

[Link]: http://emir-munoz.github.com/wikitables
