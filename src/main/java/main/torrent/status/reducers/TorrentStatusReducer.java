package main.torrent.status.reducers;

import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.DownloadState;
import main.torrent.status.state.tree.SearchPeersState;
import main.torrent.status.state.tree.TorrentFileSystemState;
import main.torrent.status.state.tree.TorrentStatusState;
import redux.reducer.Reducer;
import redux.store.Request;
import redux.store.Result;

public class TorrentStatusReducer implements Reducer<TorrentStatusState, TorrentStatusAction> {

	public static TorrentStatusState defaultTorrentState =
			new TorrentStatusState(null, TorrentStatusAction.INITIALIZE,
					DownloadStateReducer.defaultDownloadStateSupplier.get(),
					PeersStateReducer.defaultPeersStateSupplier.get(),
					TorrentFileSystemStateReducer.defaultTorrentFileSystemStateSupplier.get());

	private DownloadStateReducer downloadStateReducer = new DownloadStateReducer();
	private PeersStateReducer peersStateReducer = new PeersStateReducer();
	private TorrentFileSystemStateReducer torrentFileSystemStateReducer = new TorrentFileSystemStateReducer();

	public Result<TorrentStatusState, TorrentStatusAction> reducer(TorrentStatusState lastState, Request<TorrentStatusAction> request) {

		DownloadState downloadState = this.downloadStateReducer.reducer(lastState, request.getAction());
		SearchPeersState searchPeersState = this.peersStateReducer.reducer(lastState, request.getAction());
		TorrentFileSystemState torrentFileSystemState = this.torrentFileSystemStateReducer.reducer(lastState, request.getAction());
		if (lastState.getDownloadState().equals(downloadState) &&
				lastState.getSearchPeersState().equals(searchPeersState) &&
				lastState.getTorrentFileSystemState().equals(torrentFileSystemState))
			return new Result<>(request, lastState, false);
		TorrentStatusState newState = new TorrentStatusState(request.getId(),
				request.getAction(),
				downloadState,
                searchPeersState,
				torrentFileSystemState);
		return new Result<>(request, newState, true);
	}
}
