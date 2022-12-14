package ovh.fejker.filetosql;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@MultipartConfig
@WebServlet(name = "ImportUsersServlet", value = "/ImportUsersServlet")
public class ImportUsersServlet extends HttpServlet {
    private boolean success = false;
    private int result = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            result = 0;
            success = false;
            Part filePart = request.getPart("file");
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();      //xml upload
            InputStream fileContent = filePart.getInputStream();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);                   //xml parse
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(fileContent);
            doc.getDocumentElement().normalize();

            NodeList list = doc.getElementsByTagName("user");

            Connection connection = DatabaseHandler.getConnection();

            connection.prepareStatement("TRUNCATE TABLE users").executeUpdate();    //assuming we want to overwrite existing records
            for(int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;
                    String name = element.getElementsByTagName("name").item(0).getTextContent();        //getting data from xml
                    String surname = element.getElementsByTagName("surname").item(0).getTextContent();
                    String login = element.getElementsByTagName("login").item(0).getTextContent();
                    String query = "INSERT INTO users (name, surname, login) VALUES(?,?,?)";                //insert into database
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, name);
                    preparedStatement.setString(2, surname);
                    preparedStatement.setString(3, login);

                    result += (preparedStatement.executeUpdate()) * 3;      //get affected rows
                }
                success = true;
            }
            connection.close();

            request.setAttribute("success", success);
            request.setAttribute("result", result);

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            RequestDispatcher requestDispatcher = request.getRequestDispatcher("importusers.jsp");
            requestDispatcher.forward(request, response);
        }

    }
}
