package util;

import java.util.BitSet;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class IndentedXMLStreamWriter implements XMLStreamWriter {
	private final XMLStreamWriter xmlWriter;
	private boolean isNoFirstElement = false;
	private int indent = 0;
	private BitSet withSubElementSet = new BitSet();
	
	public IndentedXMLStreamWriter(XMLStreamWriter xmlWriter) {
		this.xmlWriter = xmlWriter;
	}

	private void indentWriteEmptyElement() throws XMLStreamException {
		if(indent > 0) {
			withSubElementSet.set(indent - 1);
		}
		writeIndent();
	}
	
	private void indentWriteStartElement() throws XMLStreamException {
		if(indent > 0) {
			withSubElementSet.set(indent - 1);
		}
		withSubElementSet.clear(indent);
		if(isNoFirstElement) {
			writeIndent();
		} else {
			isNoFirstElement = true;
		}
		indent++;
	}
	
	private void indentWriteEndElement() throws XMLStreamException {
		indent--;
		if(withSubElementSet.get(indent)) {
			writeIndent();
		}
	}
	
	private void writeIndent() throws XMLStreamException {
		int len = 1 + 2 * indent;
		char[] chars = new char[len];
		chars[0] = '\n';

		for (int i = 1; i < len; i++) {
			chars[i] = ' ';
		}
		xmlWriter.writeCharacters(chars, 0, len);
	}

	public void writeStartElement(String localName) throws XMLStreamException {
		indentWriteStartElement();
		xmlWriter.writeStartElement(localName);
	}

	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		indentWriteStartElement();
		xmlWriter.writeStartElement(namespaceURI, localName);
	}

	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		indentWriteStartElement();
		xmlWriter.writeStartElement(prefix, localName, namespaceURI);
	}

	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		indentWriteEmptyElement();
		xmlWriter.writeEmptyElement(namespaceURI, localName);
	}

	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		indentWriteEmptyElement();
		xmlWriter.writeEmptyElement(prefix, localName, namespaceURI);
	}

	public void writeEmptyElement(String localName) throws XMLStreamException {
		indentWriteEmptyElement();
		xmlWriter.writeEmptyElement(localName);
	}

	public void writeEndElement() throws XMLStreamException {
		indentWriteEndElement();
		xmlWriter.writeEndElement();
	}

	public void writeEndDocument() throws XMLStreamException {
		xmlWriter.writeEndDocument();
	}

	public void close() throws XMLStreamException {
		xmlWriter.close();
	}

	public void flush() throws XMLStreamException {
		xmlWriter.flush();
	}

	public void writeAttribute(String localName, String value) throws XMLStreamException {
		xmlWriter.writeAttribute(localName, value);
	}

	public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
			throws XMLStreamException {
		xmlWriter.writeAttribute(prefix, namespaceURI, localName, value);
	}

	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		xmlWriter.writeAttribute(namespaceURI, localName, value);
	}

	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		xmlWriter.writeNamespace(prefix, namespaceURI);
	}

	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		xmlWriter.writeDefaultNamespace(namespaceURI);
	}

	public void writeComment(String data) throws XMLStreamException {
		xmlWriter.writeComment(data);
	}

	public void writeProcessingInstruction(String target) throws XMLStreamException {
		xmlWriter.writeProcessingInstruction(target);
	}

	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		xmlWriter.writeProcessingInstruction(target, data);
	}

	public void writeCData(String data) throws XMLStreamException {
		xmlWriter.writeCData(data);
	}

	public void writeDTD(String dtd) throws XMLStreamException {
		xmlWriter.writeDTD(dtd);
	}

	public void writeEntityRef(String name) throws XMLStreamException {
		xmlWriter.writeEntityRef(name);
	}

	public void writeStartDocument() throws XMLStreamException {
		xmlWriter.writeStartDocument();
	}

	public void writeStartDocument(String version) throws XMLStreamException {
		xmlWriter.writeStartDocument(version);
	}

	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		xmlWriter.writeStartDocument(encoding, version);
	}

	public void writeCharacters(String text) throws XMLStreamException {
		xmlWriter.writeCharacters(text);
	}

	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		xmlWriter.writeCharacters(text, start, len);
	}

	public String getPrefix(String uri) throws XMLStreamException {
		return xmlWriter.getPrefix(uri);
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		xmlWriter.setPrefix(prefix, uri);
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		xmlWriter.setDefaultNamespace(uri);
	}

	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		xmlWriter.setNamespaceContext(context);
	}

	public NamespaceContext getNamespaceContext() {
		return xmlWriter.getNamespaceContext();
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return xmlWriter.getProperty(name);
	}
}