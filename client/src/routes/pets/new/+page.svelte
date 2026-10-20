<script lang="ts">
	import { goto } from '$app/navigation';
	import { createPet } from '$lib/api/pet/PetController';
	import { getOwners } from '$lib/api/owner/OwnerController';
	import { getPetTypes } from '$lib/api/pet-type/PetTypeController';
	import type { OwnerResponse, PetTypeResponse } from '$lib/api/models';
	import PetForm from '$lib/components/pets/PetForm.svelte';
	import { Button } from '$lib/components/ui/button';
	import { ArrowLeft, PawPrint } from 'lucide-svelte';
	import { toast } from 'svelte-sonner';

	let owners = $state<OwnerResponse[]>([]);
	let petTypes = $state<PetTypeResponse[]>([]);
	let loading = $state(true);
	let loadFailed = $state(false);

	async function loadFormData() {
		loading = true;
		loadFailed = false;
		try {
			const [ownerData, typeData] = await Promise.all([getOwners(), getPetTypes()]);
			owners = ownerData;
			petTypes = typeData;
		} catch (err) {
			loadFailed = true;
			toast.error('Failed to load form');
			console.error('Error:', err);
		} finally {
			loading = false;
		}
	}

	async function handleSubmit(data: { name: string; birthDate: string; typeId: number; ownerId?: number }) {
		if (!data.ownerId) return;
		try {
			const pet = await createPet({
				name: data.name,
				birthDate: data.birthDate,
				typeId: data.typeId,
				ownerId: data.ownerId
			});
			toast.success('Pet created successfully');
			goto(`/pets/${pet.id}`);
		} catch (err) {
			toast.error('Failed to create pet');
			console.error('Error:', err);
		}
	}

	$effect(() => {
		loadFormData();
	});
</script>

<svelte:head>
	<title>Add New Pet | VetHub</title>
</svelte:head>

<div class="container mx-auto max-w-2xl px-4 py-8">
	<Button variant="ghost" href="/pets" class="mb-6 gap-2">
		<ArrowLeft class="h-4 w-4" />
		Back to Pets
	</Button>

	<div class="mb-6">
		<h1 class="text-2xl font-bold">Add New Pet</h1>
		<p class="text-muted-foreground">Register a patient for an existing owner</p>
	</div>

	{#if loading}
		<div class="card p-12 text-center">
			<div class="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
			<p class="text-muted-foreground">Loading form...</p>
		</div>
	{:else if loadFailed}
		<div class="card p-12 text-center">
			<PawPrint class="mx-auto mb-4 h-12 w-12 text-muted-foreground/50" />
			<p class="text-muted-foreground">Failed to load form</p>
			<Button class="mt-4" onclick={loadFormData}>Retry</Button>
		</div>
	{:else if owners.length === 0}
		<div class="card p-12 text-center">
			<PawPrint class="mx-auto mb-4 h-12 w-12 text-muted-foreground/50" />
			<p class="text-muted-foreground">No owners registered yet. Add an owner first.</p>
		</div>
	{:else if petTypes.length === 0}
		<div class="card p-12 text-center">
			<PawPrint class="mx-auto mb-4 h-12 w-12 text-muted-foreground/50" />
			<p class="text-muted-foreground">No pet types available. Please add pet types first.</p>
		</div>
	{:else}
		<div class="card p-6">
			<PetForm {owners} {petTypes} onSubmit={handleSubmit} submitLabel="Add Pet" />
		</div>
	{/if}
</div>
