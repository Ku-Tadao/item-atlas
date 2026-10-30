package com.itematlas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * A location's access requirements as the scraper writes them, one per line: "quest:Dragon Slayer II",
 * "started:Legends' Quest" (partial completion), "skill:Agility:70" and "note:Requires a Slayer task."
 * Checks run on the client thread; anything that can't be checked (logged out, a diary, an unknown
 * quest) counts as met, so it never blocks a route.
 */
public final class LocationRequirements
{
	// "Recipe for Disaster: Sir Amik Varze" on the wiki is "Recipe for Disaster - Sir Amik Varze" in RuneLite
	private static final Map<String, Quest> QUESTS = Arrays.stream(Quest.values())
		.collect(Collectors.toMap(quest -> normalize(quest.getName()), Function.identity(), (a, b) -> a));

	private LocationRequirements()
	{
	}

	/**
	 * What the player doesn't meet, e.g. "70 Agility (you have 65)" or "Dragon Slayer II".
	 */
	public static List<String> unmet(Client client, String requirements)
	{
		List<String> unmet = new ArrayList<>();
		if (requirements == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return unmet;
		}
		for (String line : requirements.split("\n"))
		{
			String[] parts = line.split(":", 2);
			if (parts.length < 2)
			{
				continue;
			}
			if (parts[0].equals("skill"))
			{
				String[] skillLevel = parts[1].split(":");
				Skill skill = skill(skillLevel[0]);
				if (skill == null)
				{
					continue;
				}
				// a boost reaches a requirement too
				int level = Math.max(client.getRealSkillLevel(skill), client.getBoostedSkillLevel(skill));
				if (level < Integer.parseInt(skillLevel[1]))
				{
					unmet.add(skillLevel[1] + " " + skillLevel[0] + " (you have " + level + ")");
				}
			}
			else if (parts[0].equals("quest") || parts[0].equals("started"))
			{
				Quest quest = QUESTS.get(normalize(parts[1]));
				QuestState state = quest == null ? null : quest.getState(client);
				if (state == QuestState.NOT_STARTED || (parts[0].equals("quest") && state == QuestState.IN_PROGRESS))
				{
					unmet.add(parts[0].equals("quest") ? parts[1] : "start " + parts[1]);
				}
			}
		}
		return unmet;
	}

	/**
	 * The wiki's notes, for showing as-is.
	 */
	public static List<String> notes(String requirements)
	{
		return requirements == null ? List.of() : Arrays.stream(requirements.split("\n"))
			.filter(line -> line.startsWith("note:"))
			.map(line -> line.substring("note:".length()))
			.collect(Collectors.toList());
	}

	private static Skill skill(String name)
	{
		try
		{
			return Skill.valueOf(name.toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	private static String normalize(String name)
	{
		return name.toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}
