package org.apache.tuscany.sca.binding.corba.types.util;

import org.apache.tuscany.sca.binding.corba.types.TypeTreeNode;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

public class ByteTypeHelper implements TypeHelper {

	public TypeCode getType(TypeTreeNode node) {
		return ORB.init().get_primitive_tc(org.omg.CORBA.TCKind.from_int(10));
	}

	public Object read(TypeTreeNode node, InputStream is) {
		return is.read_octet();
	}

	public void write(TypeTreeNode node, OutputStream os, Object data) {
		os.write_octet((Byte) data);
	}

}
