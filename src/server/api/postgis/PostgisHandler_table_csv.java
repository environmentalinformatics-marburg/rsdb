package server.api.postgis;

import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import jakarta.servlet.http.HttpServletResponse;
import postgis.FieldsConsumer;
import postgis.PostgisLayer;
import postgis.PostgisLayer.PostgisColumn;
import util.Web;
import util.collections.array.ReadonlyArray;

public class PostgisHandler_table_csv {

	private static class FieldsConsumerCSV implements FieldsConsumer  {

		private final PrintWriter out;

		public FieldsConsumerCSV(PrintWriter out) {
			this.out = out;
		}
		
		@Override
		public void acceptFields(ReadonlyArray<PostgisColumn> fields) {
			boolean first = true;
			for (PostgisColumn field : fields) {
				if(first) {
					first = false;
				} else {
					out.print(',');
				}
				out.print(field.name);
				/*out.print('[');
				out.print(field.type);
				out.print(']');*/
			}
			out.print('\n');
		}
		
		@Override
		public void acceptStart() {
			// nothing;	
		}
		
		@Override
		public void acceptCellsStart(int i) {
			//nothing
		}

		@Override
		public void acceptCell(int i, String value) {
			if(i > 0) {
				out.print(',');
			}
			if(value.indexOf('\n') != -1) { // simple format no new line: remove new lines
				value = value.replace("\n", "");
			}
			if(value.indexOf('\\') != -1) { // simple format no escapes: remove inner escapes
				value = value.replace("\\", "");
			}
			if(value.indexOf('\"') != -1) { // simple format no inner quotes: remove inner quotes
				value = value.replace("\"", "");
			}
			if(value.indexOf(',') == -1) { // simple format allowed comma: add outer quotes if comma is present
				out.print(value);
			} else {
				out.print('\"');
				out.print(value);
				out.print('\"');
			}			
		}

		@Override
		public void acceptCellNull(int i) {
			if(i > 0) {
				out.print(',');
			}
		}
		
		@Override
		public void acceptCellInt16(int i, short value) {
			if(i > 0) {
				out.print(',');
			}
			out.print(value);			
		}

		@Override
		public void acceptCellInt32(int i, int value) {
			if(i > 0) {
				out.print(',');
			}
			out.print(value);
		}
		
		@Override
		public void acceptCellFloat64(int i, double value) {
			if(i > 0) {
				out.print(',');
			}
			out.print(value);
		}

		@Override
		public void acceptCellsEnd(int i) {
			out.print('\n');
		}

		@Override
		public void acceptEnd() {
			// nothing		
		}		
	}

	public void handle(PostgisLayer postgisLayer, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_CSV);
		PrintWriter out = response.getWriter();
		FieldsConsumer c = new FieldsConsumerCSV(out);
		postgisLayer.forEachWithFields(null, c);
	}

}
