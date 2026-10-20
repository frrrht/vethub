<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { getPets, deletePet } from '$lib/api/pet/PetController';
	import type { PetPageResponse, PetResponse } from '$lib/api/models';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Badge } from '$lib/components/ui/badge';
	import * as Table from '$lib/components/ui/table';
	import * as Dialog from '$lib/components/ui/dialog';
	import { PawPrint, Plus, Search, ChevronLeft, ChevronRight } from 'lucide-svelte';
	import { toast } from 'svelte-sonner';

	let pageResult = $state<PetPageResponse | null>(null);
	let loading = $state(true);
	let loadFailed = $state(false);
	let searchInput = $state('');
	let deleteDialogOpen = $state(false);
	let deleting = $state(false);
	let petToDelete = $state<PetResponse | null>(null);
	let requestId = 0;

	const submittedQuery = $derived($page.url.searchParams.get('q') ?? '');
	const pageIndex = $derived(parsePage($page.url.searchParams.get('page')));
	const pets = $derived(pageResult?.content ?? []);
	const totalElements = $derived(pageResult?.totalElements ?? 0);
	const totalPages = $derived(pageResult?.totalPages ?? 0);
	const from = $derived(totalElements === 0 ? 0 : (pageResult?.page ?? 0) * (pageResult?.size ?? 20) + 1);
	const to = $derived((pageResult?.page ?? 0) * (pageResult?.size ?? 20) + pets.length);
	const displayPage = $derived((pageResult?.page ?? pageIndex) + 1);

	function parsePage(value: string | null): number {
		if (!value) return 0;
		const parsed = Number(value);
		if (!Number.isInteger(parsed) || parsed < 0) return 0;
		return parsed;
	}

	function directoryHref(query: string, nextPage: number): string {
		const params = new URLSearchParams();
		if (query) params.set('q', query);
		params.set('page', String(nextPage));
		return `/pets?${params.toString()}`;
	}

	async function loadPets(query: string, currentPage: number) {
		const id = ++requestId;
		loading = true;
		loadFailed = false;
		try {
			const result = await getPets({ q: query || undefined, page: currentPage });
			if (id !== requestId) return;
			pageResult = result;
		} catch (err) {
			if (id !== requestId) return;
			loadFailed = true;
			pageResult = null;
			toast.error('Failed to load pets');
			console.error('Error loading pets:', err);
		} finally {
			if (id === requestId) loading = false;
		}
	}

	function handleSearch(event: SubmitEvent) {
		event.preventDefault();
		goto(directoryHref(searchInput.trim(), 0));
	}

	function goToPage(nextPage: number) {
		goto(directoryHref(submittedQuery, nextPage));
	}

	function openDelete(pet: PetResponse) {
		petToDelete = pet;
		deleteDialogOpen = true;
	}

	async function confirmDelete() {
		if (!petToDelete?.id) return;
		deleting = true;
		try {
			await deletePet(petToDelete.id);
			toast.success('Pet deleted successfully');
			deleteDialogOpen = false;
			const deletedWasLastOnPage = pets.length === 1 && pageIndex > 0;
			petToDelete = null;
			if (deletedWasLastOnPage) {
				goToPage(pageIndex - 1);
			} else {
				await loadPets(submittedQuery, pageIndex);
			}
		} catch (err) {
			toast.error('Failed to delete pet');
			console.error('Error deleting pet:', err);
		} finally {
			deleting = false;
		}
	}

	$effect(() => {
		searchInput = submittedQuery;
	});

	$effect(() => {
		loadPets(submittedQuery, pageIndex);
	});
</script>

<svelte:head>
	<title>Pets | VetHub</title>
</svelte:head>

<div class="container mx-auto px-4 py-8">
	<div class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
		<div class="flex items-center gap-3">
			<div class="flex h-12 w-12 items-center justify-center rounded-lg bg-accent/10">
				<PawPrint class="h-6 w-6 text-accent" />
			</div>
			<div>
				<h1 class="text-2xl font-bold text-foreground">Pets</h1>
				<p class="text-sm text-muted-foreground">Look up patients without starting from the owner</p>
			</div>
		</div>
		<Button href="/pets/new" class="gap-2">
			<Plus class="h-4 w-4" />
			Add Pet
		</Button>
	</div>

	<form class="mb-6 flex max-w-xl gap-2" onsubmit={handleSearch}>
		<div class="relative flex-1">
			<Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
			<Input
				type="search"
				placeholder="Search name, owner, or type..."
				bind:value={searchInput}
				class="pl-10"
			/>
		</div>
		<Button type="submit">Search</Button>
	</form>

	{#if loading}
		<div class="card p-12 text-center">
			<div class="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
			<p class="text-muted-foreground">Loading pets...</p>
		</div>
	{:else if loadFailed}
		<div class="card p-12 text-center">
			<PawPrint class="mx-auto mb-4 h-12 w-12 text-muted-foreground/50" />
			<p class="text-muted-foreground">Failed to load pets</p>
			<Button class="mt-4" onclick={() => loadPets(submittedQuery, pageIndex)}>Retry</Button>
		</div>
	{:else if pets.length === 0}
		<div class="card p-12 text-center">
			<PawPrint class="mx-auto mb-4 h-12 w-12 text-muted-foreground/50" />
			{#if submittedQuery}
				<p class="text-muted-foreground">No pets found matching "{submittedQuery}"</p>
			{:else}
				<p class="text-muted-foreground">No pets registered yet</p>
			{/if}
		</div>
	{:else}
		<div class="card overflow-hidden">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Name</Table.Head>
						<Table.Head>Type</Table.Head>
						<Table.Head>Born</Table.Head>
						<Table.Head>Owner</Table.Head>
						<Table.Head class="w-[220px]">Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each pets as pet (pet.id)}
						<Table.Row class="hover:bg-muted/50">
							<Table.Cell>
								<a href="/pets/{pet.id}" class="font-medium text-foreground hover:text-primary">
									{pet.name}
								</a>
							</Table.Cell>
							<Table.Cell>
								<Badge variant="secondary">{pet.type?.name ?? 'Unknown'}</Badge>
							</Table.Cell>
							<Table.Cell class="text-sm text-muted-foreground">{pet.birthDate}</Table.Cell>
							<Table.Cell>
								<a href="/owners/{pet.ownerId}" class="text-sm text-foreground hover:text-primary">
									{pet.owner?.firstName} {pet.owner?.lastName}
								</a>
							</Table.Cell>
							<Table.Cell>
								<div class="flex items-center gap-1">
									<Button variant="ghost" size="sm" href="/pets/{pet.id}">View</Button>
									<Button variant="ghost" size="sm" href="/pets/{pet.id}/edit">Edit</Button>
									<Button variant="ghost" size="sm" class="text-destructive" onclick={() => openDelete(pet)}>
										Delete
									</Button>
								</div>
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
			<div class="flex flex-col gap-3 border-t border-border px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
				<p class="text-sm text-muted-foreground">Showing {from}–{to} of {totalElements}</p>
				{#if totalPages > 1}
					<div class="flex items-center gap-2">
						<Button variant="outline" size="sm" disabled={pageIndex === 0} onclick={() => goToPage(pageIndex - 1)}>
							<ChevronLeft class="h-4 w-4" />
							Previous
						</Button>
						<span class="text-sm text-muted-foreground">Page {displayPage} of {totalPages}</span>
						<Button
							variant="outline"
							size="sm"
							disabled={pageIndex >= totalPages - 1}
							onclick={() => goToPage(pageIndex + 1)}
						>
							Next
							<ChevronRight class="h-4 w-4" />
						</Button>
					</div>
				{/if}
			</div>
		</div>
	{/if}
</div>

<Dialog.Root bind:open={deleteDialogOpen}>
	<Dialog.Content>
		<Dialog.Header>
			<Dialog.Title>Delete Pet</Dialog.Title>
			<Dialog.Description>
				Delete {petToDelete?.name}? This cannot be undone.
			</Dialog.Description>
		</Dialog.Header>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (deleteDialogOpen = false)} disabled={deleting}>Cancel</Button>
			<Button variant="destructive" onclick={confirmDelete} disabled={deleting}>
				{deleting ? 'Deleting...' : 'Delete'}
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>
