package voxeldb.voxelmapper;

import voxeldb.VoxelCell;

@FunctionalInterface
public interface VoxelMapperInt32 {
	int[][][] map(VoxelCell voxelCell);	
}