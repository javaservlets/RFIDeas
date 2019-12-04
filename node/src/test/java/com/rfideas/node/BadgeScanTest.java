package com.rfideas.node;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.commons.lang.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

import java.util.HashMap;
import java.util.Map;

@Test
public class BadgeScanTest {

    @Mock
    BadgeScan.Config config;

    @Mock
    Realm realm;

    @Spy
    @InjectMocks
    private BadgeScan node;

    @BeforeMethod
    public void before() throws IdRepoException {
        initMocks(this);
        when(config.attributeName()).thenReturn("sunIdentityMSISDNNumber");
        doReturn(new AMIdentity(null, "id=testuser,ou=user,o=ssousers,ou=services,dc=openam,dc=forgerock,dc=org")).when(
                node).getIdentity(any(), any());
    }

    public void testProcessWithTrueOutcome() {
        TreeContext context = getTreeContext(new HashMap<>());
        context.sharedState.put("q_val", "06 04 06 04 09 07 09 05 08 07 01 03 02 00 01 01");
        // WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.callbacks).isEmpty();
        assertThat(action.outcome).isEqualToIgnoringCase("TRUE");
        assertThat(StringUtils.equals(context.sharedState.get(SharedStateConstants.USERNAME).asString(), "testuser"))
                .isTrue();

    }

    public void testProcessWithFalseOutcome() {
        TreeContext context = getTreeContext(new HashMap<>());
        context.sharedState.put("EMPTY", "01234");
        // WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.callbacks).isEmpty();
        assertThat(action.outcome).isEqualToIgnoringCase("FALSE");
        assertThat(context.sharedState.get(SharedStateConstants.USERNAME).asString()).isNull();
    }

    private TreeContext getTreeContext(Map<String, String[]> parameters) {
        return new TreeContext(JsonValue.json(object(1)),
                               new ExternalRequestContext.Builder().parameters(parameters).build(), emptyList());
    }


}
