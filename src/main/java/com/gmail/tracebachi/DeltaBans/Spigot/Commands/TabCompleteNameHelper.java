package com.gmail.tracebachi.DeltaBans.Spigot.Commands;

import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class TabCompleteNameHelper {
    TabCompleteNameHelper() {
    }

    static List<String> getNamesThatStartsWith(String partialName, SockExchangeApi api) {
        List<String> results = new ArrayList(2);
        partialName = partialName.toLowerCase();
        Iterator var3 = api.getOnlinePlayerNames().iterator();

        while(var3.hasNext()) {
            String name = (String)var3.next();
            if (name.toLowerCase().startsWith(partialName)) {
                results.add(name);
            }
        }

        return results;
    }
}
