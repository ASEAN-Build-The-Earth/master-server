package asia.buildtheearth.asean.discord.api;

import asia.buildtheearth.asean.discord.abstraction.AbstractDiscordInteraction;

public interface DiscordInteraction<I extends AbstractDiscordInteraction<?>> {

    I getInteraction();
}
