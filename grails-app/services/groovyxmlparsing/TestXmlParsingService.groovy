package groovyxmlparsing

import groovy.xml.DOMBuilder
import groovy.xml.MarkupBuilder
import groovy.xml.dom.DOMCategory
import groovy.xml.XmlUtil;
import groovy.xml.XmlParser
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult


class TestXmlParsingService {

    def test_XmlSlurper() {

        def text = '''
    <list>
        <technology>
            <name>Groovy</name>
        </technology>
    </list>
'''

        def list = new XmlSlurper().parseText(text)
        println(list)
        println(list.getClass())
        println(list.class)

        assert list instanceof GPathResult
        assert list.technology.name == 'Groovy'
        String techName = list.technology.name
        println(techName)

    }


    def test_XmlParser(){

        def text = '''
    <list>
        <technology>
            <name>Groovy</name>
        </technology>
    </list>
'''

        def list = new XmlParser().parseText(text)
        println(list)
        println(list.getClass())
        println(list.class)

        assert list instanceof Node
        assert list.technology.name.text() == 'Groovy'
        String techName = list.technology.name.text()
        println(techName)

    }



    def test_DOMBuilder(){

        def CAR_RECORDS = '''
<records>
  <car name='HSV Maloo' make='Holden' year='2006'>
    <country>Australia</country>
    <record type='speed'>Production Pickup Truck with speed of 271kph</record>
  </car>
  <car name='P50' make='Peel' year='1962'>
    <country>Isle of Man</country>
    <record type='size'>Smallest Street-Legal Car at 99cm wide and 59 kg in weight</record>
  </car>
  <car name='Royale' make='Bugatti' year='1931'>
    <country>France</country>
    <record type='price'>Most Valuable Car at $15 million</record>
  </car>
</records>
'''

        def reader = new StringReader(CAR_RECORDS)
        def doc = DOMBuilder.parse(reader)
        def records = doc.documentElement

        use(DOMCategory) {
            assert records.car.size() == 3
            println(records.car.size())
        }


    }


    def test_GPath_XmlSlurper(){

        final String books = '''
    <response version-api="2.0">
        <value>
            <books>
                <book available="20" id="1">
                    <title>Don Quixote</title>
                    <author id="1">Miguel de Cervantes</author>
                </book>
                <book available="14" id="2">
                    <title>Catcher in the Rye</title>
                   <author id="2">JD Salinger</author>
               </book>
               <book available="13" id="3">
                   <title>Alice in Wonderland</title>
                   <author id="3">Lewis Carroll</author>
               </book>
               <book available="5" id="4">
                   <title>Don Quixote</title>
                   <author id="4">Miguel de Cervantes</author>
               </book>
           </books>
       </value>
    </response>
'''

        def response = new XmlSlurper().parseText(books)
        def authorResult = response.value.books.book[0].author

        println(authorResult.text())
        assert authorResult.text() == 'Miguel de Cervantes'

        // =============== more test
        def book = response.value.books.book[0]
        def bookAuthorId1 = book.@id
        def bookAuthorId2 = book['@id']

        assert bookAuthorId1 == '1'
        assert bookAuthorId1.toInteger() == 1
        assert bookAuthorId1 == bookAuthorId2

        // =============== more test
        // breadthFirst()
        // .'*' could be replaced by .children()
        def catcherInTheRye = response.value.books.'*'.find { node ->
            // node.@id == 2 could be expressed as node['@id'] == 2
            node.name() == 'book' && node.@id == '2'
        }

        println( catcherInTheRye.title.text() )
        assert catcherInTheRye.title.text() == 'Catcher in the Rye'

        // ================ more test
        // depthFirst()
        // .'**' could be replaced by .depthFirst()
        def bookId = response.'**'.find { bookx ->
            bookx.author.text() == 'Lewis Carroll'
        }.@id

        assert bookId == 3


        // ================ more test
        def titles = response.'**'.findAll { node -> node.name() == 'title' }*.text()

        assert titles.size() == 4
        println(titles)

    }


    def test_MarkupBuilder(){

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        xml.records() {
            car(name: 'HSV Maloo', make: 'Holden', year: 2006) {
                country('Australia')
                record(type: 'speed', 'Production Pickup Truck with speed of 271kph')
            }
            car(name: 'Royale', make: 'Bugatti', year: 1931) {
                country('France')
                record(type: 'price', 'Most Valuable Car at $15 million')
            }
        }

        def records = new XmlSlurper().parseText(writer.toString())

//        assert records.car.first().name.text() == 'HSV Maloo'
//        assert records.car.last().name.text() == 'Royale'

        println( records.car.first() )
        println( records.car.first()['@name'] )
        assert records.car.first()['@name'] == 'HSV Maloo'

        println( XmlUtil.serialize(records) )

    }


    def test_creating_XML_elements(){

        def xmlString = "<movie>the godfather</movie>"

        def xmlWriter = new StringWriter()
        def xmlMarkup = new MarkupBuilder(xmlWriter)

        xmlMarkup.movie("the godfather")

        assert xmlString == xmlWriter.toString()
        println( XmlUtil.serialize(xmlWriter.toString()) )

        // ============= more test
        def xmlString1 = "<movie id='2'>the godfather</movie>"

        def xmlWriter1 = new StringWriter()
        def xmlMarkup1 = new MarkupBuilder(xmlWriter1)

        xmlMarkup1.movie(id: "2", "the godfather")

        assert xmlString1 == xmlWriter1.toString()

        // ================ test more
        def xmlWriter2 = new StringWriter()
        def xmlMarkup2 = new MarkupBuilder(xmlWriter2)

        xmlMarkup2.movie(id: 2) {
            name("the godfather")
        }

        def movie2 = new XmlSlurper().parseText(xmlWriter2.toString())

        println( XmlUtil.serialize(xmlWriter2.toString()) )
        assert movie2.@id == 2
        assert movie2.name.text() == 'the godfather'


    }

    def test_NamespaceAware(){

        def xmlWriter = new StringWriter()
        def xmlMarkup = new MarkupBuilder(xmlWriter)

        xmlMarkup
                .'x:movies'('xmlns:x': 'http://www.groovy-lang.org') {
                    'x:movie'(id: 1, 'the godfather')
                    'x:movie'(id: 2, 'ronin')
                }

        def movies =
                new XmlSlurper()
                        .parseText(xmlWriter.toString())
                        .declareNamespace(x: 'http://www.groovy-lang.org')

        assert movies.'x:movie'.last().@id == 2
        assert movies.'x:movie'.last().text() == 'ronin'

    }

    def test_XmlUtil(){

        def response = new XmlParser().parseText(xml)
        def nodeToSerialize = response.'**'.find { it.name() == 'author' }
        def nodeAsText = XmlUtil.serialize(nodeToSerialize)

        assert nodeAsText ==
                XmlUtil.serialize('<?xml version="1.0" encoding="UTF-8"?><author id="1">Miguel de Cervantes</author>')
    }




    static void main( String[] args ){

        def obj = new TestXmlParsingService()
        obj.test_XmlSlurper()
        obj.test_XmlParser()
        obj.test_DOMBuilder()
        obj.test_GPath_XmlSlurper()
        obj.test_MarkupBuilder()
        obj.test_creating_XML_elements()
        obj.test_NamespaceAware()

    }


}
