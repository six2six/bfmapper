bfmapper: Java Bean Converter
=========================================

Installing:
=========================================

Clone bfmapper project and install in your local repository

mvn clean install

Use it like a maven dependency on your project

```xml
 	  <dependency>
	     <groupId>br.com.bfmapper</groupId>
		 <artifactId>bfmapper</artifactId>
		 <version>1.1.6</version>
	   </dependency>
```

Usage:
=========================================

writing bean mapper rules

```java
     public class StudentMappingRules implements RulesMapper {

      @Override
      public void loadRules() {
       MappingRules.addRule(new Converter(StudentA.class, Student.class) {{
       // not necessary to put equals name properties
       add("age", "age");
       //custom transformers
       add("birthday", "birthday").with(new DateTimeTransformer("ddMMyyyy"));
       //default values
       addDefault(Student.class, "gender", Gender.MALE);
       //chained values
       add("address", "address.address1");
       }});
     }
    }
```

converting objects:

```java
    Student student = new Mapping().apply(studentA).to(Student.class);
```

multiple applies

```java
    Student student = new Mapping().apply(studentA).apply(carMappedPrevioslyToStudent).to(Student.class);
```

You can see more utilization on tests!
