import groovy.text.GStringTemplateEngine

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom

//class Random {
////    static def Int() {
////        ThreadLocalRandom.current().nextInt()
////    }


def randomInt = (int ... bounds) -> {
    ThreadLocalRandom.current().nextInt(*bounds)
}
//    static def Int(int bounds)  {
//        ThreadLocalRandom.current().nextInt(bounds)
//    }
def randomDouble = (int ... bounds) -> {
    ThreadLocalRandom.current().nextDouble(*bounds)
}

def randomUUID = () -> {
    UUID.randomUUID().toString()
}

def randomString = (int length) -> {
    generator(('A'..'Z').join(), length)
}

def randomAlphaNumeric = (int length) -> {
    generator((('A'..'Z') + ('0'..'9')).join(), length)
}

def generator = { String alphabet, int n ->
    new Random().with {
        (1..n).collect { alphabet[nextInt(alphabet.length())] }.join()
    }
}

def currentDate = () -> {
    LocalDate.now()
}

def currentDateTime = () -> {
    LocalDateTime.now()
}

def currentDateFmt = (String fmt) -> {
    LocalDate.now().format(DateTimeFormatter.ofPattern(fmt))
}

def currentDateTimeFmt = (String fmt) -> {
    LocalDateTime.now().format(DateTimeFormatter.ofPattern(fmt))
}

def counter = () -> {
    $counter++
}
//    static def Double(int bounds)  {
//        ThreadLocalRandom.current().nextInt(bounds)
//    }
//}
//
//println(randomInt())
//println(randomInt(123))
//println(randomInt(3, 10))

//Binding binding = new Binding();
def evalTemplate = (String template) -> {
    new GStringTemplateEngine().createTemplate(template).make([
            randomInt         : randomInt,
            randomDouble      : randomDouble,
            randomUUID        : randomUUID,
            randomString      : randomString,
            randomAlphaNumeric: randomAlphaNumeric,
            currentDate       : currentDate,
            currentDateTime   : currentDateTime,
            currentDateFmt    : currentDateFmt,
            currentDateTimeFmt: currentDateTimeFmt,
            counter           : counter
    ])
}

println(evalTemplate('{{ "name": "${randomString}", "count" : ${counter}, age: ${randomInt} }}'))
