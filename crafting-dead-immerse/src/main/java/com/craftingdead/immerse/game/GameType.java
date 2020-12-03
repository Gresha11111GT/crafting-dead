/**
 * Crafting Dead
 * Copyright (C) 2020  Nexus Node
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.craftingdead.immerse.game;

import java.util.function.Function;
import java.util.function.Supplier;
import com.craftingdead.immerse.server.LogicalServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.DistExecutor.SafeCallable;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class GameType extends ForgeRegistryEntry<GameType> {

  private final Function<LogicalServer, IGameServer<?>> gameServerFactory;
  private final Supplier<SafeCallable<IGameClient<?>>> gameClientFactory;

  public GameType(Function<LogicalServer, IGameServer<?>> gameServerFactory,
      Supplier<SafeCallable<IGameClient<?>>> gameClientFactory) {
    this.gameServerFactory = gameServerFactory;
    this.gameClientFactory = gameClientFactory;
  }

  public IGameServer<?> createGameServer(LogicalServer logicalServer) {
    return this.gameServerFactory.apply(logicalServer);
  }

  public IGameClient<?> createGameClient() throws Exception {
    IGameClient<?> gameClient = DistExecutor.safeCallWhenOn(Dist.CLIENT, this.gameClientFactory);
    if (gameClient == null) {
      throw new IllegalStateException("Attempting to create game client on wrong dist");
    }
    return gameClient;
  }
}