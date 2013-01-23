package org.forgerock.openam.forgerockrest.session;

import org.forgerock.json.resource.ResultHandler;
import org.forgerock.openam.forgerockrest.session.query.SessionQueryManager;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.*;

/**
 * @author robert.wapshott@forgerock.com
 */
public class SessionResourceTest {
    @Test
    public void shouldUseSessionQueryManagerForAllSessionsQuery() {
        // Given
        String badger = "badger";
        String weasel = "weasel";

        SessionQueryManager mockManager = mock(SessionQueryManager.class);
        ResultHandler mockHandler = mock(ResultHandler.class);

        SessionResource resource = spy(new SessionResource(mockManager));
        List<String> list = Arrays.asList(new String[]{badger, weasel});
        doReturn(list).when(resource).getAllServerIds();

        // When
        resource.readInstance(null, SessionResource.KEYWORD_ALL, null, mockHandler);

        // Then
        List<String> result = Arrays.asList(new String[]{badger, weasel});
        verify(mockManager, times(1)).getAllSessions(result);
    }

    @Test
    public void shouldQueryNamedServerInServerMode() {
        // Given
        String badger = "badger";

        SessionQueryManager mockManager = mock(SessionQueryManager.class);
        ResultHandler mockHandler = mock(ResultHandler.class);

        SessionResource resource = spy(new SessionResource(mockManager));

        // When
        resource.readInstance(null, badger, null, mockHandler);

        // Then
        verify(resource, times(0)).getAllServerIds();

        List<String> result = Arrays.asList(new String[]{badger});
        verify(mockManager, times(1)).getAllSessions(result);
    }
}
