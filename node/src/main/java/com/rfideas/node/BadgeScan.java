package com.rfideas.node;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class, configClass = BadgeScan.Config.class)
public class BadgeScan extends AbstractDecisionNode {
    private final Config config;
    private final Logger logger = LoggerFactory.getLogger(BadgeScan.class);
    private String realm;


    public interface Config {
        // this field contains the badge ID for enrolled users
        @Attribute(order = 100)
        default String attributeName() { return "sunIdentityMSISDNNumber"; }
    }

    @Inject
    public BadgeScan(@Assisted Config config, @Assisted Realm realm) {
        this.config = config;
        this.realm = realm.toString();
    }

    @Override
    public Action process(TreeContext context) {
        // this tree assumes that the QueueReader node has already been hit, and has populated by a field by this
        // name (which contains the badge ID)
        String qValue = context.sharedState.get("q_val").asString();
        if (qValue == null) {
            log("No badge was found in context. Was qAuthNode configured?");
            return goTo(false).build();
        }

        Set<String> userAliasSet = new HashSet<>();
        userAliasSet.add(config.attributeName());

        // param 1: *value* of attribute to search for; param 2: realm; param 3: name of the attribute to search on
        AMIdentity username = getIdentity(qValue, userAliasSet);

        if (username == null) {
            log("No user found with that badge value");
            return goTo(false).build();
        } else {
            log("Found a badge id matching user: " + username.getName());
            // we now have the user matching the badge ID
            return goTo(true).replaceSharedState(
                    context.sharedState.put(SharedStateConstants.USERNAME, username.getName())).build();
        }
    }

    @VisibleForTesting
    AMIdentity getIdentity(String qValue, Set<String> userAliasSet) {
        return IdUtils.getIdentity(qValue, this.realm, userAliasSet);
    }

    private void log(String str) {
        logger.debug("log msg: " + str + "\r\n");
    }


}
