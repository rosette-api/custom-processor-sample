# Rosette Entity Extractor Custom Processor Sample

The Rosette Entity Extractor supports custom processors that allow you to preprocess text at the pre-extraction phase or modify the output at the pre-redaction phase. This sample will show how a custom processor can be used to correct an entity type.

Requirements:
- Rosette Enterprise 1.17.0
- Rosette Enterprise license for Entity Extraction and English
- [OPTIONAL] If you start Rosette Server via docker-compose, Docker must be installed and running on your machine

## Observe default behavior

Start Rosette Server
```
export ROSAPI_HOME=/path-to-rosette-install/server directory
$ROSAPI_HOME/bin/launch.sh console
```
Call the /entities endpoint
```
curl -s --request POST 'http://localhost:8181/rest/v1/entities' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--data '{"content":"My name is Karin", "language":"eng"}' \
| jq .
```
You will see that "Karin" is extracted with the incorrect type ORGANIZATION. The custom processor sample code will correct entities that follow the string "My name is" to be type PERSON.

## Build

To build the custom processor sample, run
```
export CP_SAMPLE=/path-to-this-directory
cd $CP_SAMPLE
mvn -Drosapi.home=$ROSAPI_HOME -P extract-rex-core-jar
mvn -Drosapi.home=$ROSAPI_HOME -P build-custom-processor
```

This will build the `custom-processor-sample-1.0.jar` in the target directory.

## Integrate with Rosette Server

Copy `custom-processor-sample-1.0.jar` into Rosette Server
```
cp target/custom-processor-sample-1.0.jar $ROSAPI_HOME/launcher/bundles
```

Edit `$ROSAPI_HOME/launcher/config/rosapi/rex-factory-config.yaml` and add the following lines

```
#Custom processors to add to annotators. See AppDev guide for more details on custom processor.
customProcessors:
    - personContextAnnotator
    - boundaryAdjustAnnotator
    - metadataAnnotator

#Register a custom processor class. See AppDev guide for more details on custom processor.
customProcessorClasses:
    - sample.SampleCustomProcessor
```

## Run

Start Rosette Server
```
$ROSAPI_HOME/bin/launch.sh console
```
Execute the same curl command as before: 
```
curl -s --request POST 'http://localhost:8181/rest/v1/entities' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--data '{"content":"My name is Karin", "language":"eng"}' \
| jq .
``` 

The output is
```
{
  "entities": [
    {
      "type": "PERSON",
      "mention": "Karin",
      "normalized": "Karin",
      "count": 1,
      "mentionOffsets": [
        {
          "startOffset": 11,
          "endOffset": 16
        }
      ],
      "entityId": "T0",
      "confidence": 0.03511071
    }
  ]
}
```
Notice that the entity type is now correct as PERSON.


## The Docker way

1. Edit the `docker-compose.yaml` file, adding the following files to the volumes section.
```
volumes:
  - rosette-roots-vol:/rosette/server/roots:ro
  - ${ROSAPI_LICENSE_PATH}:/rosette/server/launcher/config/rosapi/rosette-license.xml:ro
  - ${CP_SAMPLE}/config/rex-factory-config.yaml:/rosette/server/launcher/config/rosapi/rex-factory-config.yaml:ro
  - ${CP_SAMPLE}/target/custom-processor-sample-1.0.jar:/rosette/server/launcher/bundles/custom-processor-sample-1.0.jar:ro
```

2. Start the Rosette Server Docker container
```
ROSAPI_LICENSE_PATH=<path-to-license>/rosette-license.xml docker-compose up
```

Call the /entities endpoint with the same command as shown above. You should see "Karin" extracted
as the correct entity type, PERSON.
